package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("tmf623CreateInteractionDelegate")
@Slf4j
@RequiredArgsConstructor
public class Tmf623CreateInteractionDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        String interactionId = "INT-" + UUID.randomUUID();
        String externalId = (String) execution.getVariable("externalId");
        execution.setVariable("interactionId", interactionId);
        execution.setVariable("interactionReserved", true);
        trackingService.track(externalId, "TMF623_CREATED", "camunda7", "ORCHESTRATION_STARTED", "INTERACTION_CREATED",
                "Interaction criada: " + interactionId).subscribe();
        log.info("[TMF623] Interação criada com sucesso. interactionId={}", interactionId);
    }
}
