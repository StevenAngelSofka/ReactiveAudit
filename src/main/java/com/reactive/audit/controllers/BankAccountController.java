package com.reactive.audit.controllers;

import com.reactive.audit.DTO.req.BankAccountRequestDTO;
import com.reactive.audit.DTO.res.BankAccountResponseDTO;
import com.reactive.audit.model.BankAccount;
import com.reactive.audit.services.accounts.BankAccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@AllArgsConstructor
@Validated
public class BankAccountController {

    @Autowired
    private BankAccountService bankAccountService;

    @GetMapping(value = "/balance-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Double> streamBalance(@RequestParam String numberAccount) {
        return bankAccountService.streamBalanceByAccountNumber(numberAccount);
    }

//    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<BankAccount> streamBalance(@RequestParam String numberAccount) {
//        return bankAccountService.streamBalanceByAccountNumber(numberAccount);
//    }

    @GetMapping("")
    public Mono<ResponseEntity<BankAccountResponseDTO>> getAllAccounts() {
        return bankAccountService.getAllAccounts()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<BankAccountResponseDTO>> createAccount(@Valid @RequestBody BankAccountRequestDTO requestDTO) {
        return bankAccountService.createAccount(requestDTO)
                .map(response -> response.isSuccess()
                        ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @PutMapping("/update/{accountId}")
    public Mono<ResponseEntity<BankAccountResponseDTO>> updateAccount(@PathVariable UUID accountId, @Valid @RequestBody BankAccountRequestDTO requestDTO) {
        return bankAccountService.updateAccount(accountId, requestDTO)
                .map(response -> response.isSuccess()
                        ? ResponseEntity.ok(response)
                        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @DeleteMapping("/delete/{accountId}")
    public Mono<ResponseEntity<BankAccountResponseDTO>> deleteAccount(@PathVariable UUID accountId) {
        return bankAccountService.deleteAccount(accountId)
                .map(response -> response.isSuccess()
                        ? ResponseEntity.ok(response)
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
    }

    @PostMapping("/deposit/{accountId}")
    public Mono<ResponseEntity<BankAccountResponseDTO>> depositMoney(@PathVariable UUID accountId, @RequestParam double amount) {
        return bankAccountService.depositMoney(accountId, amount)
                .map(response -> response.isSuccess()
                        ? ResponseEntity.ok(response)
                        : ResponseEntity.badRequest().body(response));
    }

    @PostMapping("/withdraw/{accountId}")
    public Mono<ResponseEntity<BankAccountResponseDTO>> withdrawMoney(@PathVariable UUID accountId, @RequestParam double amount) {
        return bankAccountService.withdrawMoney(accountId, amount)
                .map(response -> response.isSuccess()
                        ? ResponseEntity.ok(response)
                        : ResponseEntity.badRequest().body(response));
    }
}
