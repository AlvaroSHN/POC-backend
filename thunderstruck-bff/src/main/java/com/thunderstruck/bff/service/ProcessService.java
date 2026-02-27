package com.thunderstruck.bff.service;

import com.thunderstruck.bff.model.ProcessRequest;
import com.thunderstruck.bff.repository.ProcessRequestRepository;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRequestRepository repository;
    private final ZeebeClient zeebeClient;
    private final StreamBridge streamBridge;

    public Mono<ProcessRequest> createProcess(ProcessRequest request) {
        request.setCreatedAt(LocalDateTime.now());
        request.setStatus("CREATED");
        request.setExternalId(UUID.randomUUID().toString());

        return repository.save(request)
                .doOnSuccess(savedRequest -> {
                    // 2. Publicação manual via StreamBridge (Envelope Pattern)
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
}
