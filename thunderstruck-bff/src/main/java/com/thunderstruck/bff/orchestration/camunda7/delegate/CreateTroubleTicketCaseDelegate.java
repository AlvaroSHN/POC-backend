package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.model.TroubleTicket;
import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component("createTroubleTicketCaseDelegate")
@RequiredArgsConstructor
public class CreateTroubleTicketCaseDelegate implements JavaDelegate {

    private final TroubleTicketRepository ticketRepository;
    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "CASE");
        Map<String, Object> before = SimulationSupport.snapshot(execution, "interactionItemId", "slaHours", "tipo_cliente");

        TroubleTicket ticket = TroubleTicket.builder()
                .externalId((String) execution.getVariable("externalId"))
                .description((String) execution.getVariable("description"))
                .severity("GOLD".equalsIgnoreCase((String) execution.getVariable("tipo_cliente")) ? "Critical" : "Minor")
                .status("Acknowledged")
                .createdAt(LocalDateTime.now())
                .relatedPartyId((String) execution.getVariable("origem"))
                .ticketType("Technical")
                .build();

        TroubleTicket saved = ticketRepository.save(ticket).block();
        if (saved == null) {
            throw new IllegalStateException("Não foi possível criar TroubleTicket");
        }

        execution.setVariable("caseTicketId", saved.getId());
        execution.setVariable("caseStatus", saved.getStatus());
        Map<String, Object> after = SimulationSupport.snapshot(execution, "caseTicketId", "caseStatus", "slaHours");
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "CASE_CREATED", "camunda7", "INTERACTION_ITEM_CREATED", "CASE_CREATED",
                SimulationSupport.transformDetails(before, after)).subscribe();
    }
}
