package com.reactive.audit.services.transactions;

import com.reactive.audit.DTO.req.TransactionRequestDTO;
import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Mono<TransactionResponseDTO> getAllTransactions();

    Flux<Transaction> streamTransactions(String numberAccount);

    Mono<TransactionResponseDTO> createTransaction(TransactionRequestDTO transactionRequestDTO);
}
