package com.thunderstruck.bff.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderstruck.bff.service.OrchestrationEngine;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CdcEventConsumer {

    private final OrchestrationEngine orchestrationEngine;
    private final ObjectMapper objectMapper;
    private final ProcessTrackingService trackingService;

    @Value("${thunderstruck.orchestration.process-definition-key:thunderstruck-saga-process}")
    private String processDefinitionKey;

    @Bean
    public Consumer<String> cdcConsumer() {
        return message -> {
            try {
                log.info("Recebido evento bruto do Kafka: {}", message);
                JsonNode rootNode = objectMapper.readTree(message);
                JsonNode payload = rootNode.has("payload") ? rootNode.get("payload") : rootNode;
                JsonNode after = payload.path("after");

                if (after.isMissingNode() || after.isNull()) {
                    log.warn("Evento ignorado: 'after' está vazio (pode ser um DELETE ou mensagem de controle)");
                    return;
                }

                String externalId = getField(after, "EXTERNAL_ID", "externalId");
                String description = getField(after, "DESCRIPTION", "description");
                String clientType = getField(after, "CLIENT_TYPE", "clientType");
                String origin = getField(after, "ORIGIN", "origin");

                Map<String, Object> variables = new HashMap<>();
                variables.put("externalId", externalId);
                variables.put("description", description);
                variables.put("tipo_cliente", clientType != null ? clientType : "Gold");
                variables.put("origem", origin != null ? origin : "App");
                variables.put("forceSagaFailure", false);

                trackingService.track(
                                externalId,
                                "CDC_CONSUMED",
                                "kafka-cdc",
                                "CREATED",
                                "CDC_RECEIVED",
                                "Evento Debezium processado e pronto para orquestração")
                        .subscribe();

                log.info("Encaminhando evento para o módulo de orquestração. Processo={} externalId={} variáveis={}",
                        processDefinitionKey, externalId, variables);
                orchestrationEngine.startProcess(processDefinitionKey, variables);

                trackingService.track(
                                externalId,
                                "ORCHESTRATION_STARTED",
                                "orchestration-engine",
                                "CDC_RECEIVED",
                                "ORCHESTRATION_STARTED",
                                "Instância BPMN iniciada no motor")
                        .subscribe();

            } catch (Exception e) {
                log.error("Erro crítico ao processar mensagem do Kafka: {}", e.getMessage());
            }
        };
    }

    private String getField(JsonNode node, String upper, String lower) {
        if (node.has(upper)) return node.get(upper).asText();
        if (node.has(lower)) return node.get(lower).asText();
        return null;
    }
}
