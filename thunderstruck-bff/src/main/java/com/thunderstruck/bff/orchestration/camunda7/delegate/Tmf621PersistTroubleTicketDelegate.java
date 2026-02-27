package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.model.TroubleTicket;
import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("tmf621PersistTroubleTicketDelegate")
@RequiredArgsConstructor
@Slf4j
public class Tmf621PersistTroubleTicketDelegate implements JavaDelegate {

    private final TroubleTicketRepository ticketRepository;
    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        String externalId = (String) execution.getVariable("externalId");
        String description = (String) execution.getVariable("description");
        String clientType = (String) execution.getVariable("tipo_cliente");
        String origin = (String) execution.getVariable("origem");
        String interactionItemId = (String) execution.getVariable("interactionItemId");

        TroubleTicket ticket = TroubleTicket.builder()
                .externalId(externalId)
                .description(description)
                .severity("GOLD".equalsIgnoreCase(clientType) ? "Critical" : "Minor")
                .status("Resolved")
                .createdAt(LocalDateTime.now())
                .relatedPartyId(origin)
                .ticketType("Technical")
                .interactionItemId(interactionItemId)
                .build();

        TroubleTicket saved = ticketRepository.save(ticket).block();
        if (saved != null) {
            execution.setVariable("tmf621TicketId", saved.getId());
            trackingService.track(externalId, "TMF621_PERSISTED", "camunda7", "READY_TO_PERSIST", "RESOLVED",
                    "Ticket TMF621 persistido com id=" + saved.getId()).subscribe();
            log.info("[TMF621] TroubleTicket persistido com sucesso. id={} externalId={}", saved.getId(), externalId);
        } else {
            throw new IllegalStateException("Falha ao persistir TroubleTicket no TMF621");
        }
    }
}
