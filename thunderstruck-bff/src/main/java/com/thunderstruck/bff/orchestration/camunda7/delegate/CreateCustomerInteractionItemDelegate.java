package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component("createCustomerInteractionItemDelegate")
@RequiredArgsConstructor
public class CreateCustomerInteractionItemDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "INTERACTION_ITEM");
        Map<String, Object> before = SimulationSupport.snapshot(execution, "interactionId", "description");
        execution.setVariable("interactionItemId", "ITEM-" + UUID.randomUUID());
        execution.setVariable("interactionItemStatus", "OPEN");
        Map<String, Object> after = SimulationSupport.snapshot(execution, "interactionId", "interactionItemId", "interactionItemStatus");
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "INTERACTION_ITEM_CREATED", "camunda7", "INTERACTION_CREATED", "INTERACTION_ITEM_CREATED",
                SimulationSupport.transformDetails(before, after)).subscribe();
    }
}
