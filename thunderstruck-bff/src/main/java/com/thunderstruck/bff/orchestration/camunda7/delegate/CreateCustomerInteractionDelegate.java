package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.model.CustomerInteraction;
import com.thunderstruck.bff.repository.CustomerInteractionRepository;
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
    private final CustomerInteractionRepository customerInteractionRepository;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "INTERACTION");
        Map<String, Object> before = SimulationSupport.snapshot(execution, "protocol", "externalId");
        String interactionId = "INT-" + UUID.randomUUID();
        execution.setVariable("interactionId", interactionId);
        String externalId = (String) execution.getVariable("externalId");
        String description = (String) execution.getVariable("description");
        String origin = (String) execution.getVariable("origem");

        customerInteractionRepository.save(CustomerInteraction.builder()
                        .interactionId(interactionId)
                        .externalId(externalId)
                        .creationDate(java.time.LocalDateTime.now())
                        .channel(origin)
                        .reason(description)
                        .customerId(externalId)
                        .build())
                .block();

        Map<String, Object> after = SimulationSupport.snapshot(execution, "protocol", "interactionId", "externalId");
        trackingService.track(externalId, "INTERACTION_CREATED", "camunda7", "PROTOCOL_CREATED", "INTERACTION_CREATED",
                SimulationSupport.transformDetails(before, after)).subscribe();
    }
}
