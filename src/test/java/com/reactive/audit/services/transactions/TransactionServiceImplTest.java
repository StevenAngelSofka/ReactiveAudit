package com.reactive.audit.services.transactions;

import com.reactive.audit.DTO.req.TransactionRequestDTO;
import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.repositories.TransactionReactiveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionReactiveRepository transactionReactiveRepository;


    @Test
    void testGetAllTransactions_Success() {
        // Crear una lista de transacciones mockeadas
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountId(UUID.randomUUID());
        transaction.setNumberAccount("1234567890");
        transaction.setAmount(500.0);
        transaction.setType("DEPOSIT");
        transaction.setCurrentBalance(1500.0);
        transaction.setPreviousBalance(1000.0);
        transaction.setTransactionDate(LocalDateTime.now());

        when(transactionReactiveRepository.findAll()).thenReturn(Flux.just(transaction));

        Mono<TransactionResponseDTO> response = transactionService.getAllTransactions();

        assertNotNull(response, "The response should not be null");

        StepVerifier.create(response)
                .expectNextMatches(dto -> dto.getMessage().equals("Transactions found."))
                .verifyComplete();

        verify(transactionReactiveRepository, times(1)).findAll();
    }

    @Test
    void testStreamTransactions_Success() {
        String accountNumber = "1234567890";

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountId(UUID.randomUUID());
        transaction.setNumberAccount(accountNumber);
        transaction.setAmount(500.0);
        transaction.setType("DEPOSIT");
        transaction.setCurrentBalance(1500.0);
        transaction.setPreviousBalance(1000.0);
        transaction.setTransactionDate(LocalDateTime.now());

        when(transactionReactiveRepository.findWithTailableCursorByNumberAccount(accountNumber)).thenReturn(Flux.just(transaction));

        Flux<Transaction> response = transactionService.streamTransactions(accountNumber);

        StepVerifier.create(response)
                .expectNext(transaction)
                .verifyComplete();

        verify(transactionReactiveRepository, times(1)).findWithTailableCursorByNumberAccount(accountNumber);
    }

    @Test
    void testCreateTransaction_Success() {
        // Crear TransactionRequestDTO utilizando el builder
        TransactionRequestDTO requestDTO = TransactionRequestDTO.builder()
                .accountId(UUID.randomUUID())
                .numberAccount("1234567890")
                .type("DEPOSIT")
                .amount(500.0)
                .previousBalance(1000.0)
                .currentBalance(1500.0)
                .build();

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(UUID.randomUUID());
        savedTransaction.setAccountId(requestDTO.getAccountId());
        savedTransaction.setNumberAccount(requestDTO.getNumberAccount());
        savedTransaction.setAmount(requestDTO.getAmount());
        savedTransaction.setType(requestDTO.getType());
        savedTransaction.setPreviousBalance(requestDTO.getPreviousBalance());
        savedTransaction.setCurrentBalance(requestDTO.getCurrentBalance());
        savedTransaction.setTransactionDate(LocalDateTime.now());

        // Mock del repositorio
        when(transactionReactiveRepository.save(any(Transaction.class))).thenReturn(Mono.just(savedTransaction));

        // Llamada al servicio
        Mono<TransactionResponseDTO> response = transactionService.createTransaction(requestDTO);

        // Verificación de la respuesta
        StepVerifier.create(response)
                .expectNextMatches(dto -> dto.getMessage().equals("Transaction created successfully."))
                .verifyComplete();

        // Verificar que el repositorio haya sido llamado
        verify(transactionReactiveRepository, times(1)).save(any(Transaction.class));
    }


}