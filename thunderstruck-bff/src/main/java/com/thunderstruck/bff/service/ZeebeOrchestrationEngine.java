package com.thunderstruck.bff.service;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "thunderstruck.orchestration.engine", havingValue = "zeebe")
public class ZeebeOrchestrationEngine implements OrchestrationEngine {

    private final ZeebeClient zeebeClient;
    private final ProcessTrackingService trackingService;

    @Override
    public void startProcess(String processDefinitionKey, Map<String, Object> variables) {
        String externalId = String.valueOf(variables.get("externalId"));
        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(processDefinitionKey)
                .latestVersion()
                .variables(variables)
                .send()
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        trackingService.track(externalId,
                                "ZEEBE_INSTANCE_CREATED",
                                "zeebe-runtime",
                                "ORCHESTRATION_REQUESTED",
                                "ORCHESTRATION_STARTED",
                                "ProcessInstanceKey=" + result.getProcessInstanceKey()).subscribe();
                        log.info("[ORCHESTRATION] Instância criada no motor para processo '{}' (key={})",
                                processDefinitionKey,
                                result.getProcessInstanceKey());
                    } else {
                        log.error("[ORCHESTRATION] Falha ao iniciar processo '{}'", processDefinitionKey, exception);
                    }
                });
    }
}
