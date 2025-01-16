package com.reactive.audit.repositories;

import com.reactive.audit.model.BankAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import java.util.UUID;

public interface BankAccountReactiveRepository extends ReactiveMongoRepository<BankAccount, UUID> {
}
