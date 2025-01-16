package com.reactive.audit.repositories;

import com.reactive.audit.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface TransactionReactiveRepository extends ReactiveMongoRepository<Transaction, UUID> {
    @Tailable
    Flux<Transaction> findWithTailableCursorByNumberAccount(String numberAccount);
}
