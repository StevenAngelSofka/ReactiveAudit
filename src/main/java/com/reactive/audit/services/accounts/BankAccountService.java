package com.reactive.audit.services.accounts;

import com.reactive.audit.DTO.req.BankAccountRequestDTO;
import com.reactive.audit.DTO.res.BankAccountResponseDTO;
import com.reactive.audit.model.BankAccount;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BankAccountService {

    Flux<Double> streamBalanceByAccountNumber(String numberAccount);

    Mono<BankAccountResponseDTO> getAllAccounts();

    Mono<BankAccountResponseDTO> createAccount(BankAccountRequestDTO requestDTO);

    Mono<BankAccountResponseDTO> updateAccount(UUID accountId, BankAccountRequestDTO requestDTO);

    Mono<BankAccountResponseDTO> deleteAccount(UUID accountId);

    Mono<BankAccountResponseDTO> depositMoney(UUID accountId, double amount);

    Mono<BankAccountResponseDTO> withdrawMoney(UUID accountId, double amount);
}
