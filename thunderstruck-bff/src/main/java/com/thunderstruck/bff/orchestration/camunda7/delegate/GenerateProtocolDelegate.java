package com.thunderstruck.bff.orchestration.camunda7.delegate;

import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component("generateProtocolDelegate")
@RequiredArgsConstructor
@Slf4j
public class GenerateProtocolDelegate implements JavaDelegate {

    private final ProcessTrackingService trackingService;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationSupport.failIfRequested(execution, "PROTOCOL");
        Map<String, Object> before = SimulationSupport.snapshot(execution, "externalId", "description", "origem");
        String protocol = "PRT-" + ThreadLocalRandom.current().nextInt(100000, 999999);
        execution.setVariable("protocol", protocol);
        Map<String, Object> after = SimulationSupport.snapshot(execution, "externalId", "protocol", "description", "origem");
        String externalId = (String) execution.getVariable("externalId");
        trackingService.track(externalId, "PROTOCOL_CREATED", "camunda7", "ORCHESTRATION_STARTED", "PROTOCOL_CREATED",
                SimulationSupport.transformDetails(before, after)).subscribe();
        log.info("Protocolo criado: {}", protocol);
    }
}
