package com.thunderstruck.bff.repository;

import com.thunderstruck.bff.model.CustomerInteractionTopic;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CustomerInteractionTopicRepository extends ReactiveCrudRepository<CustomerInteractionTopic, Long> {

    Flux<CustomerInteractionTopic> findByInteractionId(String interactionId);
}
