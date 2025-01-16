package com.reactive.audit.services.accounts;

import com.reactive.audit.DTO.req.BankAccountRequestDTO;
import com.reactive.audit.DTO.req.TransactionRequestDTO;
import com.reactive.audit.DTO.res.BankAccountResponseDTO;
import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.BankAccount;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.repositories.BankAccountReactiveRepository;
import com.reactive.audit.repositories.TransactionReactiveRepository;
import com.reactive.audit.services.transactions.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @InjectMocks
    private BankAccountServiceImpl bankAccountService;

    @Mock
    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionReactiveRepository transactionReactiveRepository;

    @Mock
    private BankAccountReactiveRepository bankAccountReactiveRepository;

    @Test
    void testStreamBalanceByAccountNumber_Success() {
        Flux<Transaction> mockFlux = Flux.just(
                new Transaction(UUID.randomUUID(), UUID.randomUUID(), "123456789", "DEPOSIT", 50.0, 50.0, 100.0, LocalDateTime.now()),
                new Transaction(UUID.randomUUID(), UUID.randomUUID(), "123456789", "DEPOSIT", 50.0, 100.0, 150.0, LocalDateTime.now()),
                new Transaction(UUID.randomUUID(), UUID.randomUUID(), "123456789", "DEPOSIT", 50.0, 150.0, 200.0, LocalDateTime.now())
        );

        // Configurar el comportamiento del mock
        when(transactionReactiveRepository.findWithTailableCursorByNumberAccount("123456789"))
                .thenReturn(mockFlux);

        // Act: Llamar al método del servicio
        Flux<Double> responseBody = bankAccountService.streamBalanceByAccountNumber("123456789");

        // Assert: Verificar los valores emitidos por el flujo
        StepVerifier.create(responseBody)
                .expectNext(100.0) // Primer balance
                .expectNext(150.0) // Segundo balance
                .expectNext(200.0) // Tercer balance
                .verifyComplete(); // Verificar que el flujo se complete sin errores

        // Verificar que el mock fue invocado
        verify(transactionReactiveRepository, times(1))
                .findWithTailableCursorByNumberAccount("123456789");
    }

    @Test
    void testStreamBalanceByAccountNumber_NoTransactions() {
        // Arrange: Configurar el mock para devolver un Flux vacío
        when(transactionReactiveRepository.findWithTailableCursorByNumberAccount("123456789"))
                .thenReturn(Flux.empty());

        // Act: Llamar al método del servicio
        Flux<Double> responseBody = bankAccountService.streamBalanceByAccountNumber("123456789");

        // Assert: Verificar que el servicio emite un error con el mensaje esperado
        StepVerifier.create(responseBody)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("No transactions found for account 123456789"))
                .verify();

        // Verificar que el mock fue invocado
        verify(transactionReactiveRepository, times(1))
                .findWithTailableCursorByNumberAccount("123456789");
    }

    @Test
    void testGetAllAccounts_Success() {
        // Arrange: Configuración del mock con una lista de cuentas simuladas
        List<BankAccount> mockAccounts = List.of(
                new BankAccount(UUID.randomUUID(), "123456789", 1000.0, "SAVINGS"),
                new BankAccount(UUID.randomUUID(), "987654321", 500.0, "CHECKING")
        );

        when(bankAccountReactiveRepository.findAll()).thenReturn(Flux.fromIterable(mockAccounts));

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.getAllAccounts();

        // Assert: Verificar que el resultado contiene los datos esperados
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Accounts found.", response.getMessage());
                    assertEquals(2, ((List<?>) response.getData()).size());

                    // Validar los detalles de las cuentas retornadas
                    List<BankAccount> responseAccounts = (List<BankAccount>) response.getData();
                    assertEquals("123456789", responseAccounts.get(0).getNumberAccount());
                    assertEquals("987654321", responseAccounts.get(1).getNumberAccount());
                })
                .verifyComplete();

        // Verificar que el repositorio fue llamado una vez
        verify(bankAccountReactiveRepository, times(1)).findAll();
    }

    @Test
    void testCreateAccount_Success() {
        // Arrange: Datos de entrada y simulación de la cuenta guardada
        BankAccountRequestDTO requestDTO = BankAccountRequestDTO.builder()
                .numberAccount("123456789")
                .balance(1000.0)
                .type("SAVINGS")
                .build();

        BankAccount savedAccount = new BankAccount(
                UUID.randomUUID(),
                "123456789",
                1000.0,
                "SAVINGS"
        );

        when(bankAccountReactiveRepository.save(any(BankAccount.class))).thenReturn(Mono.just(savedAccount));

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.createAccount(requestDTO);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Account created successfully.", response.getMessage());
                    BankAccount responseAccount = (BankAccount) response.getData();

                    // Validar detalles de la cuenta creada
                    assertEquals("123456789", responseAccount.getNumberAccount());
                    assertEquals(1000.0, responseAccount.getBalance());
                    assertEquals("SAVINGS", responseAccount.getType());
                })
                .verifyComplete();

        // Verificar que el repositorio fue llamado con los datos correctos
        verify(bankAccountReactiveRepository, times(1)).save(any(BankAccount.class));
    }

    @Test
    void testUpdateAccount_Success() {
        // Arrange: Datos de entrada y configuración de mocks
        UUID accountId = UUID.randomUUID();
        BankAccountRequestDTO requestDTO = BankAccountRequestDTO.builder()
                .numberAccount("987654321")
                .balance(5000.0)
                .type("CHECKING")
                .build();

        BankAccount existingAccount = new BankAccount(
                accountId,
                "123456789",
                1000.0,
                "SAVINGS"
        );

        BankAccount updatedAccount = new BankAccount(
                accountId,
                "987654321",
                5000.0,
                "CHECKING"
        );

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(bankAccountReactiveRepository.save(any(BankAccount.class))).thenReturn(Mono.just(updatedAccount));

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.updateAccount(accountId, requestDTO);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Account updated successfully.", response.getMessage());
                    BankAccount responseAccount = (BankAccount) response.getData();

                    // Validar detalles de la cuenta actualizada
                    assertEquals("987654321", responseAccount.getNumberAccount());
                    assertEquals(5000.0, responseAccount.getBalance());
                    assertEquals("CHECKING", responseAccount.getType());
                })
                .verifyComplete();

        // Verificar que los métodos del repositorio fueron llamados
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(1)).save(existingAccount);
    }

    @Test
    void testUpdateAccount_AccountNotFound() {
        // Arrange: Datos de entrada
        UUID accountId = UUID.randomUUID();
        BankAccountRequestDTO requestDTO = BankAccountRequestDTO.builder()
                .numberAccount("987654321")
                .balance(5000.0)
                .type("CHECKING")
                .build();

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.empty());

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.updateAccount(accountId, requestDTO);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertFalse(response.isSuccess());
                    assertEquals("Account not found.", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        // Verificar que los métodos del repositorio fueron llamados
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(0)).save(any(BankAccount.class));
    }

    @Test
    void testDeleteAccount_Success() {
        // Arrange: Datos de entrada y configuración de mocks
        UUID accountId = UUID.randomUUID();
        BankAccount existingAccount = new BankAccount(
                accountId,
                "123456789",
                1000.0,
                "SAVINGS"
        );

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(bankAccountReactiveRepository.delete(existingAccount)).thenReturn(Mono.empty());

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.deleteAccount(accountId);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Account deleted successfully.", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        // Verificar que los métodos del repositorio fueron llamados
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(1)).delete(existingAccount);
    }

    @Test
    void testDeleteAccount_AccountNotFound() {
        // Arrange: Datos de entrada
        UUID accountId = UUID.randomUUID();

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.empty());

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.deleteAccount(accountId);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertFalse(response.isSuccess());
                    assertEquals("Account not found.", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        // Verificar que los métodos del repositorio fueron llamados
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(0)).delete(any(BankAccount.class));
    }

    @Test
    void testDepositMoney_Success() {
        // Arrange: Datos de entrada y configuración de mocks
        UUID accountId = UUID.randomUUID();
        double depositAmount = 500.0;
        BankAccount existingAccount = new BankAccount(
                accountId,
                "123456789",
                1000.0,
                "SAVINGS"
        );

        BankAccount updatedAccount = new BankAccount(
                accountId,
                "123456789",
                1500.0,
                "SAVINGS"
        );

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(bankAccountReactiveRepository.save(updatedAccount)).thenReturn(Mono.just(updatedAccount));

        // Mock para el DTO de transacción utilizando builder
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .accountId(accountId)
                .numberAccount("123456789")
                .type("DEPOSIT")
                .amount(depositAmount)
                .previousBalance(existingAccount.getBalance())
                .currentBalance(updatedAccount.getBalance())
                .build();

        // Mock de la creación de la transacción
        TransactionResponseDTO transactionResponseDTO = TransactionResponseDTO.buildSuccess(
                "Transaction created successfully.",
                new Transaction(UUID.randomUUID(), accountId, "123456789", "DEPOSIT", depositAmount, 1000.0, 1500.0, LocalDateTime.now())
        );

        when(transactionService.createTransaction(transactionRequestDTO)).thenReturn(Mono.just(transactionResponseDTO));

        // Act: Llamada al método del servicio
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.depositMoney(accountId, depositAmount);

        // Assert: Validación del resultado
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Transaction type: DEPOSIT. Amount: 500.0 . Current Balance: 1500.0", response.getMessage());
                    assertNotNull(response.getData());
                })
                .verifyComplete();

        // Verificar que los métodos del repositorio fueron llamados
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(1)).save(existingAccount);
        verify(bankAccountReactiveRepository, times(1)).save(updatedAccount);
        verify(transactionService, times(1)).createTransaction(transactionRequestDTO);

    }

    @Test
    void testWithdrawMoney_Success() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        double withdrawalAmount = 500.0;
        BankAccount existingAccount = new BankAccount(accountId, "123456789", 1500.0, "SAVINGS");
        BankAccount updatedAccount = new BankAccount(accountId, "123456789", 1000.0, "SAVINGS");

        when(bankAccountReactiveRepository.findById(accountId)).thenReturn(Mono.just(existingAccount));
        when(bankAccountReactiveRepository.save(existingAccount)).thenReturn(Mono.just(updatedAccount));

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .accountId(accountId)
                .numberAccount("123456789")
                .type("WITHDRAWAL")
                .amount(withdrawalAmount)
                .previousBalance(existingAccount.getBalance())
                .currentBalance(updatedAccount.getBalance())
                .build();

        TransactionResponseDTO transactionResponseDTO = TransactionResponseDTO.buildSuccess(
                "Transaction created successfully.",
                new Transaction(UUID.randomUUID(), accountId, "123456789", "WITHDRAWAL", withdrawalAmount, 1500.0, 1000.0, LocalDateTime.now())
        );

        when(transactionService.createTransaction(transactionRequestDTO)).thenReturn(Mono.just(transactionResponseDTO));

        // Act
        Mono<BankAccountResponseDTO> responseMono = bankAccountService.withdrawMoney(accountId, withdrawalAmount);

        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("Transaction type: WITHDRAWAL. Amount: 500.0 . Current Balance: 1000.0", response.getMessage());
                    assertNotNull(response.getData());
                })
                .verifyComplete();

        // Verificación
        verify(bankAccountReactiveRepository, times(1)).findById(accountId);
        verify(bankAccountReactiveRepository, times(1)).save(existingAccount);
        verify(bankAccountReactiveRepository, times(1)).save(updatedAccount);
        verify(transactionService, times(1)).createTransaction(transactionRequestDTO);
    }


}