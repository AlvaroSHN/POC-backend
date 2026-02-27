package com.thunderstruck.bff.service;

import com.thunderstruck.bff.model.ProcessRequest;
import com.thunderstruck.bff.repository.ProcessRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRequestRepository repository;
    private final StreamBridge streamBridge;
    private final OrchestrationEngine orchestrationEngine;
    private final ProcessTrackingService trackingService;

    @Value("${thunderstruck.orchestration.process-definition-key:thunderstruck-saga-process}")
    private String processDefinitionKey;

    public Mono<ProcessRequest> createProcess(ProcessRequest request) {
        request.setCreatedAt(LocalDateTime.now());
        request.setStatus("CREATED");
        request.setExternalId(request.getExternalId() == null || request.getExternalId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getExternalId());

        return repository.save(request)
                .flatMap(savedRequest -> trackingService.track(
                                savedRequest.getExternalId(),
                                "API_RECEIVED",
                                "bff-api",
                                null,
                                "CREATED",
                                "before={externalId=null} | after={externalId=" + savedRequest.getExternalId() + ", clientType=" + savedRequest.getClientType() + "}")
                        .thenReturn(savedRequest))
                .doOnSuccess(savedRequest -> {
                    ProcessRequest.KafkaEnvelope envelope = ProcessRequest.KafkaEnvelope.builder()
                            .metadata(ProcessRequest.Metadata.builder()
                                    .eventId(UUID.randomUUID().toString())
                                    .eventType("ProcessCreated")
                                    .timestamp(LocalDateTime.now())
                                    .source("thunderstruck-bff")
                                    .build())
                            .data(savedRequest)
                            .build();

                    streamBridge.send("processRequestProducer-out-0", envelope);
                    log.info("Mensagem manual enviada ao Kafka para ID: {}", savedRequest.getId());
                });
    }

    public Mono<ProcessRequest> simulateProcess(ProcessRequest request, String simulateFailAt) {
        request.setCreatedAt(LocalDateTime.now());
        request.setStatus("SIMULATED");
        request.setExternalId(request.getExternalId() == null || request.getExternalId().isBlank()
                ? "SIM-" + UUID.randomUUID()
                : request.getExternalId());

        return repository.save(request)
                .flatMap(saved -> trackingService.track(
                                saved.getExternalId(),
                                "SIMULATION_START",
                                "frontend-panel",
                                "SIMULATED",
                                "ORCHESTRATION_REQUESTED",
                                "before={description=" + saved.getDescription() + "} | after={simulateFailAt=" + simulateFailAt + "}")
                        .thenReturn(saved))
                .doOnSuccess(saved -> {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("externalId", saved.getExternalId());
                    variables.put("description", saved.getDescription());
                    variables.put("tipo_cliente", saved.getClientType() != null ? saved.getClientType() : "GOLD");
                    variables.put("origem", saved.getOrigin() != null ? saved.getOrigin() : "APP");
                    variables.put("simulateFailAt", simulateFailAt != null ? simulateFailAt : "NONE");
                    variables.put("issueCategory", "BILLING");
                    orchestrationEngine.startProcess(processDefinitionKey, variables);
                });
    }
}
