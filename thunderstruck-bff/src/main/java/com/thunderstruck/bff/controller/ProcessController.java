package com.thunderstruck.bff.controller;

import com.thunderstruck.bff.model.ProcessHistoryEvent;
import com.thunderstruck.bff.model.ProcessRequest;
import com.thunderstruck.bff.model.TroubleTicket;
import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessService;
import com.thunderstruck.bff.service.ProcessTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final TroubleTicketRepository ticketRepository;
    private final ProcessTrackingService trackingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProcessRequest> create(@RequestBody ProcessRequest request) {
        return processService.createProcess(request);
    }

    @PostMapping("/simulate")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProcessRequest> simulate(@RequestBody SimulationRequest request) {
        ProcessRequest processRequest = ProcessRequest.builder()
                .externalId(request.externalId())
                .description(request.description())
                .clientType(request.clientType())
                .origin(request.origin())
                .build();
        return processService.simulateProcess(processRequest, request.forceSagaFailure());
    }

    @GetMapping("/tickets")
    public Flux<TroubleTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @GetMapping("/{externalId}/history")
    public Flux<ProcessHistoryEvent> getHistory(@PathVariable String externalId) {
        return trackingService.history(externalId);
    }

    @GetMapping("/{externalId}/status")
    public Mono<Map<String, String>> getCurrentStatus(@PathVariable String externalId) {
        return trackingService.currentStatus(externalId)
                .map(status -> Map.of("externalId", externalId, "currentStatus", status));
    }

    public record SimulationRequest(String externalId,
                                    String description,
                                    String clientType,
                                    String origin,
                                    boolean forceSagaFailure) {
    }
}
