package com.thunderstruck.bff.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "thunderstruck.orchestration.engine", havingValue = "camunda7", matchIfMissing = true)
public class Camunda7OrchestrationEngine implements OrchestrationEngine {

    private final RuntimeService runtimeService;
    private final ProcessTrackingService trackingService;

    @Override
    public void startProcess(String processDefinitionKey, Map<String, Object> variables) {
        var processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
        String externalId = String.valueOf(variables.get("externalId"));
        trackingService.track(externalId,
                "CAMUNDA7_INSTANCE_CREATED",
                "camunda7-runtime",
                "ORCHESTRATION_REQUESTED",
                "ORCHESTRATION_STARTED",
                "ProcessInstanceId=" + processInstance.getProcessInstanceId()).subscribe();
        log.info("[ORCHESTRATION-C7] Instância iniciada no Camunda 7 para processo '{}' (instanceId={})",
                processDefinitionKey,
                processInstance.getProcessInstanceId());
    }
}
