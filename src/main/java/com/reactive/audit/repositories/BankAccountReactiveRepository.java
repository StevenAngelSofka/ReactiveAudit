package com.reactive.audit.repositories;

import com.reactive.audit.model.BankAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface BankAccountReactiveRepository extends ReactiveMongoRepository<BankAccount, UUID> {
    @Tailable
    Flux<BankAccount> findWithTailableCursorByNumberAccount(String numberAccount);
}

