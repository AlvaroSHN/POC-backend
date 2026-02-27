package com.thunderstruck.bff.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CdcEventConsumer {

    private final ZeebeClient zeebeClient;
    private final ObjectMapper objectMapper;

    @Bean
    public Consumer<String> cdcConsumer() {
        return message -> {
            try {
                log.info("Recebido evento bruto do Kafka: {}", message);
                
                // 1. Faz o parse manual para evitar erros de deserialização automática
                JsonNode rootNode = objectMapper.readTree(message);
                
                // 2. Navega no envelope do Debezium (payload -> after)
                // O Debezium pode enviar o dado direto ou dentro de um envelope 'payload'
                JsonNode payload = rootNode.has("payload") ? rootNode.get("payload") : rootNode;
                JsonNode after = payload.path("after");

                if (after.isMissingNode() || after.isNull()) {
                    log.warn("Evento ignorado: 'after' está vazio (pode ser um DELETE ou mensagem de controle)");
                    return;
                }

                // 3. Extração cuidadosa (Oracle costuma usar CAIXA ALTA)
                // Tentamos CAIXA ALTA primeiro, depois minúscula como fallback
                String externalId = getField(after, "EXTERNAL_ID", "externalId");
                String description = getField(after, "DESCRIPTION", "description");
                String clientType = getField(after, "CLIENT_TYPE", "clientType");
                String origin = getField(after, "ORIGIN", "origin");

                // 4. Preparação das variáveis para o Camunda
                Map<String, Object> variables = new HashMap<>();
                variables.put("externalId", externalId);
                variables.put("description", description);
                variables.put("tipo_cliente", clientType != null ? clientType : "Gold");
                variables.put("origem", origin != null ? origin : "App");

                log.info("Iniciando instância no Camunda para Processo: {} com variáveis: {}", externalId, variables);

                // 5. Comando para o Zeebe
                zeebeClient.newCreateInstanceCommand()
                        .bpmnProcessId("thunderstruck-process")
                        .latestVersion()
                        .variables(variables)
                        .send()
                        .whenComplete((result, exception) -> {
                            if (exception == null) {
                                log.info("Sucesso! Instância criada no Camunda. Key: {}", result.getProcessInstanceKey());
                            } else {
                                log.error("Falha ao criar instância no Camunda", exception);
                            }
                        });

            } catch (Exception e) {
                log.error("Erro crítico ao processar mensagem do Kafka: {}", e.getMessage());
            }
        };
    }

    // Helper para lidar com a variação de nomes de colunas do Oracle
    private String getField(JsonNode node, String upper, String lower) {
        if (node.has(upper)) return node.get(upper).asText();
        if (node.has(lower)) return node.get(lower).asText();
        return null;
    }
}
