package com.thunderstruck.bff.worker;

import com.thunderstruck.bff.model.TroubleTicket;
import com.thunderstruck.bff.repository.TroubleTicketRepository;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor // <-- IMPORTANTE: Isso injeta o repositório automaticamente
public class ThunderStruckWorkers {

    private static final Logger log = LoggerFactory.getLogger(ThunderStruckWorkers.class);
    
    // Injeção do repositório para salvar no banco Oracle
    private final TroubleTicketRepository ticketRepository;

    // =========================================================================
    // 1. WORKER DE PERSISTÊNCIA TMF621
    // =========================================================================
    @JobWorker(type = "tmf-ticket-persistence", autoComplete = true)
    public void handleTicketPersistence(final ActivatedJob job) { // Mudou de Mono<Void> para void
        Map<String, Object> variables = job.getVariablesAsMap();
        
        TroubleTicket ticket = TroubleTicket.builder()
                .externalId((String) variables.getOrDefault("externalId", "N/A"))
                .description((String) variables.getOrDefault("description", "Sem descrição"))
                .severity(mapearSeveridade((String) variables.get("tipo_cliente")))
                .status("Acknowledged")
                .createdAt(LocalDateTime.now())
                .relatedPartyId((String) variables.get("origem"))
                .ticketType("Technical")
                .build();

        log.info("[WORKER TMF] Persistindo Trouble Ticket padrão TMF621 para ID: {}", ticket.getExternalId());

        // A MÁGICA REATIVA ACONTECE AQUI NO .subscribe()
        ticketRepository.save(ticket)
                .subscribe(
                        saved -> log.info("[WORKER TMF] Ticket salvo com sucesso ID TMF: {}", saved.getId()),
                        error -> log.error("[WORKER TMF] ERRO CRÍTICO ao salvar ticket: {}", error.getMessage())
                );
    }

    // Exemplo de ACL (Camada Anti-Corrupção) para transformar os dados
    private String mapearSeveridade(String perfil) {
        if ("GOLD".equalsIgnoreCase(perfil)) return "Critical";
        return "Minor";
    }

    // =========================================================================
    // 2. WORKER PARA A OPERAÇÃO PRINCIPAL
    // =========================================================================
    @JobWorker(type = "main-operation", autoComplete = true)
    public void handleMainOperation(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String externalId = (String) variables.get("externalId");
        String cliente = (String) variables.get("tipo_cliente");

        log.info("[WORKER MAIN] Iniciando operação principal para ID: {}", externalId);
        log.info("   - Perfil do Cliente: {}", cliente);
        
        try {
            Thread.sleep(2000); // Simulando um processamento demorado
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("[WORKER MAIN] Operação concluída com sucesso!");
    }

    // =========================================================================
    // 3. WORKER PARA O ESCALONAMENTO (SLA Expirado)
    // =========================================================================
    @JobWorker(type = "escalation-handler", autoComplete = true)
    public void handleEscalation(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String externalId = (String) variables.get("externalId");

        log.warn("[WORKER ESCALATION] SLA estourou para o ID: {}! Iniciando protocolo de escalonamento.", externalId);
        log.info("[WORKER ESCALATION] Alerta enviado. Encerrando processo escalonado.");
    }

    // =========================================================================
    // 4. WORKER PARA ATUALIZAR STATUS DO TICKET
    // =========================================================================
    @JobWorker(type = "update-ticket-status", autoComplete = true)
    public void handleStatusUpdate(final ActivatedJob job) {
        String externalId = (String) job.getVariablesAsMap().get("externalId");
        String acao = (String) job.getVariablesAsMap().getOrDefault("acao", "concluir"); // Fallback seguro

        // Mapeamento dinâmico (A Camada Anti-Corrupção traduzindo para TMF621)
        String novoStatus;
        if ("escalar".equalsIgnoreCase(acao)) {
            novoStatus = "Escalated";
        } else {
            novoStatus = "Resolved"; // Se for 'concluir' ou vazio, resolve
        }

        log.info("[WORKER UPDATE] Analisando ação '{}'. Atualizando ticket {} para o status: {}", acao, externalId, novoStatus);

        // Busca o ticket, altera o status e salva de forma reativa
        ticketRepository.findByExternalId(externalId)
                .flatMap(ticket -> {
                    ticket.setStatus(novoStatus);
                    return ticketRepository.save(ticket);
                })
                .subscribe(
                        saved -> log.info("[WORKER UPDATE] Sucesso! Ticket {} agora está {}", saved.getExternalId(), saved.getStatus()),
                        error -> log.error("[WORKER UPDATE] Erro Crítico ao atualizar ticket: {}", error.getMessage())
                );
    }
}