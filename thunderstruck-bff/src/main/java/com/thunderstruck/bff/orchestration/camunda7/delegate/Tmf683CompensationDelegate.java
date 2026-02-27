package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("tmf683CompensationDelegate")
@Slf4j
@RequiredArgsConstructor
public class Tmf683CompensationDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        Object caseId = execution.getVariable("caseId");
        String externalId = (String) execution.getVariable("externalId");
        log.warn("[TMF683-COMPENSATION] Case revertido. caseId={}", caseId);
        trackingService.track(externalId, "TMF683_COMPENSATED", "camunda7", "ROLLBACK_TRIGGERED", "ROLLBACK_IN_PROGRESS",
                "Case revertido: " + caseId).subscribe();
        execution.setVariable("caseCompensated", true);
    }
}
