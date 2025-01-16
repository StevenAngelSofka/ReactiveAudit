package com.reactive.audit.controllers;

import com.reactive.audit.DTO.res.TransactionResponseDTO;
import com.reactive.audit.model.Transaction;
import com.reactive.audit.services.transactions.TransactionService;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(TransactionController.class)
@AllArgsConstructor
class TransactionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void testGetAllTransactions_Success() {
        // Arrange: Configuración de la respuesta simulada de servicio
        Transaction transaction1 = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1234567890",
                "DEPOSIT",
                500.0,
                1000.0,
                1500.0,
                LocalDateTime.now()
        );
        Transaction transaction2 = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "0987654321",
                "WITHDRAWAL",
                200.0,
                1500.0,
                1300.0,
                LocalDateTime.now()
        );

        // Simulamos que el servicio devuelve una lista de transacciones
        List<Transaction> mockTransactions = List.of(transaction1, transaction2);
        TransactionResponseDTO mockResponse = TransactionResponseDTO.buildSuccess("Transactions found.", mockTransactions);

        when(transactionService.getAllTransactions()).thenReturn(Mono.just(mockResponse));

        // Act: Realizar la solicitud GET
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/api/transactions")
                .exchange();

        // Assert: Validación de la respuesta
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200
                .expectBody(TransactionResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertTrue(response.isSuccess());  // Verifica que el campo 'success' sea verdadero
                    assertEquals("Transactions found.", response.getMessage());  // Verifica que el mensaje sea correcto
                    assertNotNull(response.getData());  // Verifica que 'data' no sea nulo
                    assertFalse(((List<?>) response.getData()).isEmpty());  // Verifica que haya transacciones
                });

        verify(transactionService, times(1)).getAllTransactions();// Verifica que el servicio fue llamado una vez
    }

    @Test
    void testStreamTransactions_Success() {
        // Arrange: Configuración del mock para el flujo de transacciones
        Transaction transaction1 = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1234567890",
                "DEPOSIT",
                500.0,
                1000.0,
                1500.0,
                LocalDateTime.now()
        );
        Transaction transaction2 = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1234567890",
                "WITHDRAWAL",
                200.0,
                1500.0,
                1300.0,
                LocalDateTime.now()
        );

        // Creamos un Flux de transacciones
        Flux<Transaction> mockFlux = Flux.just(transaction1, transaction2);

        // Simulamos que el servicio devuelve el flujo de transacciones
        when(transactionService.streamTransactions("1234567890")).thenReturn(mockFlux);

        // Act: Realización de la solicitud GET con el parámetro 'numberAccount'
        Flux<Transaction> responseBody = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/transactions/stream")
                        .queryParam("numberAccount", "1234567890")
                        .build())
                .exchange()
                .expectStatus().isOk()  // Verifica que el estado HTTP sea 200
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)  // Acepta 'text/event-stream;charset=UTF-8' también
                .returnResult(Transaction.class)  // Esperamos un flujo de objetos Transaction
                .getResponseBody();  // Obtenemos el cuerpo del flujo

        // Assert: Validación del flujo de datos
        StepVerifier.create(responseBody)
                .expectNext(transaction1)  // Esperamos que la primera transacción sea 'transaction1'
                .expectNext(transaction2)  // Luego, esperamos 'transaction2'
                .verifyComplete();  // Verifica que el flujo se complete sin errores

        verify(transactionService, times(1)).streamTransactions("1234567890");  // Verifica que el servicio fue llamado una vez
    }
}