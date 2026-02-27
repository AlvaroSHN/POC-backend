package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("updateInteractionItemFailureDelegate")
@RequiredArgsConstructor
public class UpdateInteractionItemFailureDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("interactionItemStatus", "FAILED_RETRY_ALLOWED");
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "INTERACTION_ITEM_UPDATED", "camunda7", "ROLLBACK_IN_PROGRESS", "ROLLED_BACK",
                "Interaction item marcado para nova tentativa: FAILED_RETRY_ALLOWED").subscribe();
    }
}
