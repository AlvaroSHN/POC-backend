package com.thunderstruck.bff.service;

import com.thunderstruck.bff.model.ProcessHistoryEvent;
import com.thunderstruck.bff.repository.ProcessHistoryEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProcessTrackingService {

    private final ProcessHistoryEventRepository historyRepository;

    public Mono<ProcessHistoryEvent> track(String externalId,
                                           String stage,
                                           String source,
                                           String previousStatus,
                                           String currentStatus,
                                           String details) {
        ProcessHistoryEvent event = ProcessHistoryEvent.builder()
                .externalId(externalId)
                .stage(stage)
                .source(source)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        return historyRepository.save(event);
    }

    public Flux<ProcessHistoryEvent> history(String externalId) {
        return historyRepository.findByExternalIdOrderByCreatedAtAsc(externalId);
    }

    public Mono<String> currentStatus(String externalId) {
        return history(externalId)
                .last()
                .map(ProcessHistoryEvent::getCurrentStatus)
                .defaultIfEmpty("UNKNOWN");
    }
}
