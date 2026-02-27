package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component("createCustomerInteractionDelegate")
@RequiredArgsConstructor
public class CreateCustomerInteractionDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "INTERACTION");
        Map<String, Object> before = SimulationSupport.snapshot(execution, "protocol", "externalId");
        execution.setVariable("interactionId", "INT-" + UUID.randomUUID());
        Map<String, Object> after = SimulationSupport.snapshot(execution, "protocol", "interactionId", "externalId");
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "INTERACTION_CREATED", "camunda7", "PROTOCOL_CREATED", "INTERACTION_CREATED",
                SimulationSupport.transformDetails(before, after)).subscribe();
    }
}
