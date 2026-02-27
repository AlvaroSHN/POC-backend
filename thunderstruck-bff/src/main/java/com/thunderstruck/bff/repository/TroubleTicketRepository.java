package com.thunderstruck.bff.repository;

import com.thunderstruck.bff.model.TroubleTicket;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

// Essa é a linha que faltou para o Java entender o que é o Mono:
import reactor.core.publisher.Mono; 

@Repository
public interface TroubleTicketRepository extends ReactiveCrudRepository<TroubleTicket, Long> {
    
    // O Spring cria a query SQL automaticamente baseado no nome do método!
    Mono<TroubleTicket> findByExternalId(String externalId);
}