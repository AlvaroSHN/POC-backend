package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("rollbackCaseDelegate")
@RequiredArgsConstructor
public class RollbackCaseDelegate implements JavaDelegate {

    private final TroubleTicketRepository ticketRepository;
    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        Object caseTicketId = execution.getVariable("caseTicketId");
        if (caseTicketId instanceof Number n) {
            ticketRepository.deleteById(n.longValue()).block();
        }
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "CASE_ROLLED_BACK", "camunda7", "CASE_CREATED", "ROLLBACK_IN_PROGRESS",
                "Rollback do caso executado. caseTicketId=" + caseTicketId).subscribe();
    }
}
