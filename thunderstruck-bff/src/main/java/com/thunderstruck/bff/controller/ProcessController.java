package com.thunderstruck.bff.controller;

import com.thunderstruck.bff.model.ProcessRequest;
import com.thunderstruck.bff.model.TroubleTicket;
import com.thunderstruck.bff.repository.TroubleTicketRepository;
import com.thunderstruck.bff.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final TroubleTicketRepository ticketRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProcessRequest> create(@RequestBody ProcessRequest request) {
        return processService.createProcess(request);
    }

    @GetMapping("/tickets")
    public Flux<TroubleTicket> getAllTickets() {
        return ticketRepository.findAll();
    }
}