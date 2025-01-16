package com.reactive.audit.services.accounts;

import com.reactive.audit.DTO.req.BankAccountRequestDTO;
import com.reactive.audit.DTO.req.TransactionRequestDTO;
import com.reactive.audit.DTO.res.BankAccountResponseDTO;
import com.reactive.audit.model.BankAccount;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.repositories.BankAccountReactiveRepository;
import com.reactive.audit.repositories.TransactionReactiveRepository;
import com.reactive.audit.services.transactions.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@AllArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    @Autowired
    private BankAccountReactiveRepository bankAccountReactiveRepository;

    @Autowired
    private TransactionReactiveRepository transactionReactiveRepository;

    @Autowired
    private TransactionService transactionService;

    @Override
    public Flux<Double> streamBalanceByAccountNumber(String numberAccount) {
        return transactionReactiveRepository.findWithTailableCursorByNumberAccount(numberAccount)
                .map(Transaction::getCurrentBalance)
                .switchIfEmpty(Flux.error(new RuntimeException("No transactions found for account " + numberAccount)));
    }

//    @Override
//    public Flux<BankAccount> streamBalanceByAccountNumber(String numberAccount) {
//        return bankAccountReactiveRepository.findWithTailableCursorByNumberAccount(numberAccount);
//    }


    @Override
    public Mono<BankAccountResponseDTO> getAllAccounts() {
        return bankAccountReactiveRepository.findAll()
                .collectList()
                .map(accounts -> BankAccountResponseDTO.buildSuccess("Accounts found.", accounts));
    }

    @Override
    public Mono<BankAccountResponseDTO> createAccount(BankAccountRequestDTO account) {
        BankAccount newAccount = new BankAccount();
        newAccount.setNumberAccount(account.getNumberAccount());
        newAccount.setBalance(account.getBalance());
        newAccount.setType(account.getType());

        return bankAccountReactiveRepository.save(newAccount)
                .map(savedAccount -> BankAccountResponseDTO.buildSuccess("Account created successfully.", savedAccount));
    }

    @Override
    public Mono<BankAccountResponseDTO> updateAccount(UUID accountId, BankAccountRequestDTO account) {
        return bankAccountReactiveRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setNumberAccount(account.getNumberAccount());
                    existingAccount.setBalance(account.getBalance());
                    existingAccount.setType(account.getType());

                    return bankAccountReactiveRepository.save(existingAccount)
                            .map(updatedAccount -> BankAccountResponseDTO.buildSuccess("Account updated successfully.", updatedAccount));
                })
                .defaultIfEmpty(BankAccountResponseDTO.buildError("Account not found."));
    }

    @Override
    public Mono<BankAccountResponseDTO> deleteAccount(UUID accountId) {
        return bankAccountReactiveRepository.findById(accountId)
                .flatMap(existingAccount ->
                    bankAccountReactiveRepository.delete(existingAccount)
                        .then(Mono.just(BankAccountResponseDTO.buildSuccess("Account deleted successfully.", null)))
                )
                .defaultIfEmpty(BankAccountResponseDTO.buildError("Account not found."));
    }

    @Override
    public Mono<BankAccountResponseDTO> depositMoney(UUID accountId, double amount) {
        if (amount <= 0) {
            return Mono.just(BankAccountResponseDTO.buildError("Invalid deposit amount"));
        }

        return bankAccountReactiveRepository.findById(accountId)
                .flatMap(account -> {
                    String numberAccount = account.getNumberAccount();
                    double previousBalance = account.getBalance();
                    double newBalance = previousBalance + amount;

                    account.setBalance(newBalance);

                    return bankAccountReactiveRepository.save(account)
                            .flatMap(savedAccount ->
                                createTransactionAndRespond(
                                    "DEPOSIT",
                                    accountId,
                                    numberAccount,
                                    amount,
                                    previousBalance,
                                    newBalance,
                                    savedAccount
                                )
                            );
                }).
                defaultIfEmpty(BankAccountResponseDTO.buildError("Account not found."));
    }

    @Override
    public Mono<BankAccountResponseDTO> withdrawMoney(UUID accountId, double amount) {
        if (amount <= 0) {
            return Mono.just(BankAccountResponseDTO.buildError("Invalid withdrawal amount."));
        }

        return bankAccountReactiveRepository.findById(accountId)
                .flatMap(account -> {
                    double previousBalance = account.getBalance();
                    String numberAccount = account.getNumberAccount();

                    if (amount > previousBalance) {
                        return Mono.just(BankAccountResponseDTO.buildError("Insufficient funds."));
                    }

                    double newBalance = previousBalance - amount;
                    account.setBalance(newBalance);

                    return bankAccountReactiveRepository.save(account)
                            .flatMap(savedAccount ->
                                createTransactionAndRespond(
                                    "WITHDRAWAL",
                                    accountId,
                                    numberAccount,
                                    amount,
                                    previousBalance,
                                    newBalance,
                                    savedAccount
                                )
                            );
                })
                .defaultIfEmpty(BankAccountResponseDTO.buildError("Account not found."));
    }

    private Mono<BankAccountResponseDTO> createTransactionAndRespond(
            String type, UUID accountId, String numberAccount, double amount,
            double previousBalance, double newBalance, BankAccount savedAccount) {

        return transactionService.createTransaction(TransactionRequestDTO.builder()
                        .accountId(accountId)
                        .numberAccount(numberAccount)
                        .type(type)
                        .amount(amount)
                        .previousBalance(previousBalance)
                        .currentBalance(newBalance)
                        .build())
                .thenReturn(BankAccountResponseDTO.buildSuccess(
                        "Transaction type: " + type + ". Amount: " + amount + " . Current Balance: " + newBalance, savedAccount));
    }


}
