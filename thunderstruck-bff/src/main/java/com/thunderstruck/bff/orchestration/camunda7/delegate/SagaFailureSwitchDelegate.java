package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("sagaFailureSwitchDelegate")
@Slf4j
@RequiredArgsConstructor
public class SagaFailureSwitchDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        boolean forceFail = Boolean.TRUE.equals(execution.getVariable("forceSagaFailure"));
        String externalId = (String) execution.getVariable("externalId");
        execution.setVariable("deveFalhar", forceFail);
        trackingService.track(externalId, "SAGA_DECISION", "camunda7", "CASE_CREATED",
                forceFail ? "ROLLBACK_TRIGGERED" : "READY_TO_PERSIST",
                "Decision gateway avaliou forceSagaFailure=" + forceFail).subscribe();
        log.info("[SAGA] Chave de teste forceSagaFailure={} -> deveFalhar={}", forceFail, forceFail);
    }
}
