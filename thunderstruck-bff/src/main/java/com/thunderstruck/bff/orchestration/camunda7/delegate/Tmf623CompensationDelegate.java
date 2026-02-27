package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("tmf623CompensationDelegate")
@Slf4j
@RequiredArgsConstructor
public class Tmf623CompensationDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        Object interactionId = execution.getVariable("interactionId");
        String externalId = (String) execution.getVariable("externalId");
        log.warn("[TMF623-COMPENSATION] Desfazendo interação. interactionId={}", interactionId);
        trackingService.track(externalId, "TMF623_COMPENSATED", "camunda7", "ROLLBACK_IN_PROGRESS", "ROLLED_BACK",
                "Interaction desfeita: " + interactionId).subscribe();
        execution.setVariable("interactionCompensated", true);
    }
}
