package com.reactive.audit.services.transactions;

import com.reactive.audit.DTO.req.TransactionRequestDTO;
import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.repositories.TransactionReactiveRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionReactiveRepository transactionReactiveRepository;

    @Override
    public Mono<TransactionResponseDTO> getAllTransactions() {
        return transactionReactiveRepository.findAll()
                .collectList()
                .map(transactions -> TransactionResponseDTO.buildSuccess("Transactions found.", transactions));

    }

    @Override
    public Flux<Transaction> streamTransactions(String numberAccount) {
        return transactionReactiveRepository.findWithTailableCursorByNumberAccount(numberAccount);
    }


    @Override
    public Mono<TransactionResponseDTO> createTransaction(TransactionRequestDTO transaction) {
        Transaction newTransaction = new Transaction();
        newTransaction.setAccountId(transaction.getAccountId());
        newTransaction.setNumberAccount(transaction.getNumberAccount());
        newTransaction.setAmount(transaction.getAmount());
        newTransaction.setType(transaction.getType());
        newTransaction.setCurrentBalance(transaction.getCurrentBalance());
        newTransaction.setPreviousBalance(transaction.getPreviousBalance());
        newTransaction.setTransactionDate(LocalDateTime.now());

        return transactionReactiveRepository.save(newTransaction)
                .map(savedTransaction -> TransactionResponseDTO.buildSuccess("Transaction created successfully.", savedTransaction));
    }
}
