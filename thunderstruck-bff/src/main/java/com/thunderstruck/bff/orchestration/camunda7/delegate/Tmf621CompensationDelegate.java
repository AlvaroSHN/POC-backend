package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("tmf621CompensationDelegate")
@RequiredArgsConstructor
@Slf4j
public class Tmf621CompensationDelegate implements JavaDelegate {

    private final TroubleTicketRepository ticketRepository;
    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        Object id = execution.getVariable("tmf621TicketId");
        String externalId = (String) execution.getVariable("externalId");
        if (id instanceof Number number) {
            ticketRepository.deleteById(number.longValue()).block();
            log.warn("[TMF621-COMPENSATION] Ticket removido por rollback SAGA. id={}", id);
        }
        trackingService.track(externalId, "TMF621_COMPENSATED", "camunda7", "ROLLBACK_TRIGGERED", "ROLLED_BACK",
                "Compensação TMF621 concluída").subscribe();
        execution.setVariable("tmf621Compensated", true);
    }
}
