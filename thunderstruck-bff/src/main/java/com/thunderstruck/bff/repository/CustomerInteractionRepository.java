package com.thunderstruck.bff.repository;

import com.thunderstruck.bff.model.CustomerInteraction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerInteractionRepository extends ReactiveCrudRepository<CustomerInteraction, Long> {

    Mono<CustomerInteraction> findByInteractionId(String interactionId);

    Flux<CustomerInteraction> findByExternalId(String externalId);
}
