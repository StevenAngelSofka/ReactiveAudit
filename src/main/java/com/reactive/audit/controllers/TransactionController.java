package com.reactive.audit.controllers;

import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.services.transactions.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transactions")
@AllArgsConstructor
@Validated
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("")
    public Mono<ResponseEntity<TransactionResponseDTO>> getAllTransactions() {
        return transactionService.getAllTransactions()
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Transaction> streamTransactions(@RequestParam String numberAccount) {
        return transactionService.streamTransactions(numberAccount);
    }

}
