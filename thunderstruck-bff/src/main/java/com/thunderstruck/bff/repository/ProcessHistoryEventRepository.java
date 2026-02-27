package com.thunderstruck.bff.repository;

import com.thunderstruck.bff.model.ProcessHistoryEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProcessHistoryEventRepository extends ReactiveCrudRepository<ProcessHistoryEvent, Long> {
    Flux<ProcessHistoryEvent> findByExternalIdOrderByCreatedAtAsc(String externalId);
}
