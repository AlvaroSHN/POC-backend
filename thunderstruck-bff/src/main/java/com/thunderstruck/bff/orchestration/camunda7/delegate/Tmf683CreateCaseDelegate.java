package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("tmf683CreateCaseDelegate")
@Slf4j
@RequiredArgsConstructor
public class Tmf683CreateCaseDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        String caseId = "CASE-" + UUID.randomUUID();
        String externalId = (String) execution.getVariable("externalId");
        execution.setVariable("caseId", caseId);
        execution.setVariable("caseCreated", true);
        trackingService.track(externalId, "TMF683_CREATED", "camunda7", "INTERACTION_CREATED", "CASE_CREATED",
                "Case criado: " + caseId).subscribe();
        log.info("[TMF683] Case criado para interactionId={} -> caseId={}", execution.getVariable("interactionId"), caseId);
    }
}
