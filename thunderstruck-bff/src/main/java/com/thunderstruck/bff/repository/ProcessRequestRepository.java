package com.thunderstruck.bff.repository;

import com.thunderstruck.bff.model.ProcessRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessRequestRepository extends ReactiveCrudRepository<ProcessRequest, Long> {
}
