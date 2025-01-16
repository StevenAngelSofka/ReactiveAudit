package com.reactive.audit.controllers;

import com.reactive.audit.DTO.req.BankAccountRequestDTO;
import com.reactive.audit.DTO.res.BankAccountResponseDTO;
import com.reactive.audit.model.BankAccount;
import com.reactive.audit.services.accounts.BankAccountService;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(BankAccountController.class)
@AllArgsConstructor
class BankAccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BankAccountService bankAccountService;


    @Test
    void  testStreamBalance_Success() {
        //        //Arrange: Configuración del mock para el flujo de balances
        Flux<Double> mockFlux = Flux.just(100.0, 150.0, 200.0);

        when(bankAccountService.streamBalanceByAccountNumber("123456789"))
                .thenReturn(mockFlux);

        // Act: Realización de la acción
        Flux<Double> responseBody = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/accounts/balance-stream")
                        .queryParam("numberAccount", "123456789")
                        .build())
                .exchange()
                .expectStatus().isOk()  // Verifica el estado HTTP
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)  // Verifica que el tipo de contenido sea compatible
                .returnResult(Double.class)  // Esperamos un flujo de objetos Double
                .getResponseBody();  // Obtenemos el cuerpo del flujo

        // Assert: Validación del flujo de datos
        StepVerifier.create(responseBody)
                .expectNext(100.0)  // Esperamos que el primer balance sea 100.0
                .expectNext(150.0)  // Luego, esperamos 150.0
                .expectNext(200.0)  // Y finalmente, 200.0
                .verifyComplete();  // Verifica que el flujo se complete sin errores

        verify(bankAccountService, times(1)).streamBalanceByAccountNumber("123456789");  // Verifica que el servicio haya sido llamado una vez
    }


    @Test
    void testGetAllAccounts_Success() {
        // Arrange: Configuración de datos y mocks
        List<BankAccount> mockAccounts = List.of(
                new BankAccount(UUID.randomUUID(), "1234567890", 1000.0, "SAVINGS"),
                new BankAccount(UUID.randomUUID(), "0987654321", 2000.0, "CHECKING")
        );
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Accounts found.", mockAccounts);

        when(bankAccountService.getAllAccounts()).thenReturn(Mono.just(mockResponse));

        // Act: Realización de la acción
        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/api/accounts")
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200 OK
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea de tipo BankAccountResponseDTO
                .value(response -> {
                    assertTrue(response.isSuccess());  // Verifica que el campo 'success' sea verdadero
                    assertEquals("Accounts found.", response.getMessage());  // Verifica que el mensaje sea el esperado
                    assertNotNull(response.getData());  // Verifica que los datos no sean nulos
                    assertEquals(2, ((List<?>) response.getData()).size());  // Verifica que la lista de cuentas tenga 2 elementos
                });

        verify(bankAccountService, times(1)).getAllAccounts();  // Verifica que el método del servicio haya sido llamado una vez
    }


    @Test
    void testCreateAccount_Success() {
        // Arrange: Configuración de la solicitud válida
        BankAccountRequestDTO validRequest = BankAccountRequestDTO.builder()
                .numberAccount("1234567890")
                .balance(1500.0)
                .type("CHECKING")
                .build();

        // Simulamos una cuenta bancaria creada con los valores proporcionados
        BankAccount createdAccount = new BankAccount(
                UUID.randomUUID(),
                "1234567890",
                1500.0,
                "CHECKING"
        );

        // Creamos la respuesta con el objeto creado en el campo 'data'
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Account created successfully.", createdAccount);

        // Simulamos que el servicio 'createAccount' devuelve la respuesta
        when(bankAccountService.createAccount(any(BankAccountRequestDTO.class)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/api/accounts/create")
                .bodyValue(validRequest)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isCreated()  // Verifica que el estado HTTP sea 201 (CREATED)
                .expectBody()
                .jsonPath("$.data.id").isNotEmpty()
                .jsonPath("$.data.numberAccount").isEqualTo("1234567890")
                .jsonPath("$.data.balance").isEqualTo(1500.0)
                .jsonPath("$.data.type").isEqualTo("CHECKING");

        // Verifica que el servicio fue llamado una vez
        verify(bankAccountService, times(1)).createAccount(any(BankAccountRequestDTO.class));
    }


    @Test
    void testCreateAccount_Error() {
        // Arrange: Configuración de la solicitud inválida (por ejemplo, número de cuenta ya existe)
        BankAccountRequestDTO invalidRequest = BankAccountRequestDTO.builder()
                .numberAccount("1234567890")  // Número de cuenta que ya existe
                .balance(1500.0)
                .type("CHECKING")
                .build();

        // Creamos una respuesta de error
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildError("Account already exists.");

        // Simulamos que el servicio 'createAccount' devuelve un error
        when(bankAccountService.createAccount(any(BankAccountRequestDTO.class)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/api/accounts/create")
                .bodyValue(invalidRequest)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isBadRequest()  // Verifica que el estado HTTP sea 400 (BAD REQUEST)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertFalse(response.isSuccess());  // Verifica que el campo 'success' sea falso
                    assertEquals("Account already exists.", response.getMessage());  // Verifica que el mensaje sea el error correcto
                    assertNull(response.getData());  // Verifica que 'data' sea nulo en caso de error
                });

        verify(bankAccountService, times(1)).createAccount(any(BankAccountRequestDTO.class));  // Verifica que el servicio fue llamado una vez
    }

    @Test
    void testUpdateAccount_Success() {
        // Arrange: Configuración de la solicitud válida y la respuesta exitosa
        UUID accountId = UUID.randomUUID();  // ID de cuenta simulado
        BankAccountRequestDTO validRequest = BankAccountRequestDTO.builder()
                .numberAccount("1234567890")
                .balance(1500.0)
                .type("CHECKING")
                .build();

        // Simulamos una cuenta bancaria actualizada con los nuevos valores
        BankAccount updatedAccount = new BankAccount(
                accountId,
                "1234567890",
                1500.0,
                "CHECKING"
        );

        // Creamos la respuesta con el objeto actualizado en el campo 'data'
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Account updated successfully.", updatedAccount);

        // Simulamos que el servicio 'updateAccount' devuelve la respuesta
        when(bankAccountService.updateAccount(eq(accountId), any(BankAccountRequestDTO.class)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud PUT
        WebTestClient.ResponseSpec responseSpec = webTestClient.put()
                .uri("/api/accounts/update/{accountId}", accountId)
                .bodyValue(validRequest)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200
                .expectBody()  // Verifica que la respuesta no sea nula
                .jsonPath("$.data.id").isEqualTo(updatedAccount.getId().toString())  // Verifica que 'id' coincida
                .jsonPath("$.data.numberAccount").isEqualTo(updatedAccount.getNumberAccount())  // Verifica el número de cuenta
                .jsonPath("$.data.balance").isEqualTo(updatedAccount.getBalance())  // Verifica el balance
                .jsonPath("$.data.type").isEqualTo(updatedAccount.getType());  // Verifica el tipo de cuenta

        // Verifica que el servicio fue llamado una vez
        verify(bankAccountService, times(1)).updateAccount(eq(accountId), any(BankAccountRequestDTO.class));
    }

    @Test
    void testUpdateAccount_Error() {
        // Arrange: Configuración de la solicitud válida pero el ID no existe en la base de datos
        UUID accountId = UUID.randomUUID();  // ID de cuenta que no existe
        BankAccountRequestDTO validRequest = BankAccountRequestDTO.builder()
                .numberAccount("1234567890")
                .balance(1500.0)
                .type("CHECKING")
                .build();

        // Creamos una respuesta de error para cuando no se encuentra la cuenta
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildError("Account not found.");

        // Simulamos que el servicio 'updateAccount' devuelve un error si no encuentra la cuenta
        when(bankAccountService.updateAccount(eq(accountId), any(BankAccountRequestDTO.class)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud PUT
        WebTestClient.ResponseSpec responseSpec = webTestClient.put()
                .uri("/api/accounts/update/{accountId}", accountId)
                .bodyValue(validRequest)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isNotFound()  // Verifica que el estado HTTP sea 404 (NOT FOUND)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertFalse(response.isSuccess());  // Verifica que el campo 'success' sea falso
                    assertEquals("Account not found.", response.getMessage());  // Verifica que el mensaje sea el error correcto
                    assertNull(response.getData());  // Verifica que 'data' sea nulo en caso de error
                });

        verify(bankAccountService, times(1)).updateAccount(eq(accountId), any(BankAccountRequestDTO.class));
    }

    @Test
    void testDeleteAccount_Success() {
        // Arrange: Crear una cuenta para simular la eliminación
        UUID accountId = UUID.randomUUID();  // ID simulado de la cuenta a eliminar

        // Crear una respuesta de éxito que simule la eliminación
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Account deleted successfully.", null);

        // Simular que el servicio 'deleteAccount' devuelve la respuesta de éxito
        when(bankAccountService.deleteAccount(eq(accountId)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud DELETE
        WebTestClient.ResponseSpec responseSpec = webTestClient.delete()
                .uri("/api/accounts/delete/{accountId}", accountId)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200 (OK)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertTrue(response.isSuccess());  // Verifica que 'success' sea verdadero
                    assertEquals("Account deleted successfully.", response.getMessage());  // Verifica que el mensaje sea correcto
                    assertNull(response.getData());  // Verifica que 'data' sea nulo en una eliminación exitosa
                });

        verify(bankAccountService, times(1)).deleteAccount(eq(accountId));  // Verifica que el servicio fue llamado una vez
    }

    @Test
    void testDeleteAccount_NotFound() {
        // Arrange: Crear una cuenta con un ID que no existe
        UUID accountId = UUID.randomUUID();  // ID de cuenta que no existe en la base de datos

        // Crear una respuesta de error que simule el caso de cuenta no encontrada
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildError("Account not found.");

        // Simular que el servicio 'deleteAccount' devuelve la respuesta de error
        when(bankAccountService.deleteAccount(eq(accountId)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud DELETE
        WebTestClient.ResponseSpec responseSpec = webTestClient.delete()
                .uri("/api/accounts/delete/{accountId}", accountId)
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isNotFound()  // Verifica que el estado HTTP sea 404 (NOT FOUND)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertFalse(response.isSuccess());  // Verifica que 'success' sea falso
                    assertEquals("Account not found.", response.getMessage());  // Verifica que el mensaje sea el esperado
                    assertNull(response.getData());  // Verifica que 'data' sea nulo cuando la cuenta no se encuentra
                });

        verify(bankAccountService, times(1)).deleteAccount(eq(accountId));  // Verifica que el servicio fue llamado una vez
    }


    @Test
    void testDepositMoney_Success() {
        // Arrange: Definir un ID de cuenta y un monto válido
        UUID accountId = UUID.randomUUID();  // ID simulado de la cuenta
        double depositAmount = 100.0;  // Monto del depósito

        // Crear una respuesta de éxito que simule el depósito
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Deposit successful.", null);

        // Simular que el servicio 'depositMoney' devuelve la respuesta de éxito
        when(bankAccountService.depositMoney(eq(accountId), eq(depositAmount)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST para depositar dinero
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/accounts/deposit/{accountId}")
                        .queryParam("amount", depositAmount)
                        .build(accountId))  // Agregar el parámetro `amount` en la URL
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200 (OK)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertTrue(response.isSuccess());  // Verifica que 'success' sea verdadero
                    assertEquals("Deposit successful.", response.getMessage());  // Verifica que el mensaje sea correcto
                    assertNull(response.getData());  // Verifica que 'data' sea nulo en una operación exitosa
                });

        verify(bankAccountService, times(1)).depositMoney(eq(accountId), eq(depositAmount));
    }

    @Test
    void testDepositMoney_BadRequest() {
        // Arrange: Definir un ID de cuenta y un monto negativo
        UUID accountId = UUID.randomUUID();  // ID simulado de la cuenta
        double depositAmount = -100.0;  // Monto negativo (depósito no válido)

        // Crear una respuesta de error que simule el fallo en el depósito
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildError("Amount must be positive.");

        // Simular que el servicio 'depositMoney' devuelve la respuesta de error
        when(bankAccountService.depositMoney(eq(accountId), eq(depositAmount)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST para depositar dinero
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/accounts/deposit/{accountId}")
                        .queryParam("amount", depositAmount)
                        .build(accountId))  // Agregar el parámetro `amount` en la URL
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isBadRequest()  // Verifica que el estado HTTP sea 400 (BAD REQUEST)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertFalse(response.isSuccess());  // Verifica que 'success' sea falso
                    assertEquals("Amount must be positive.", response.getMessage());  // Verifica que el mensaje sea el esperado
                    assertNull(response.getData());  // Verifica que 'data' sea nulo cuando ocurre un error
                });

        verify(bankAccountService, times(1)).depositMoney(eq(accountId), eq(depositAmount));  // Verifica que el servicio fue llamado una vez
    }


    @Test
    void testWithdrawMoney_Success() {
        // Arrange: Definir un ID de cuenta y un monto válido
        UUID accountId = UUID.randomUUID();  // ID simulado de la cuenta
        double withdrawAmount = 100.0;  // Monto del retiro

        // Crear una respuesta de éxito que simule el retiro
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildSuccess("Withdrawal successful.", null);

        // Simular que el servicio 'withdrawMoney' devuelve la respuesta de éxito
        when(bankAccountService.withdrawMoney(eq(accountId), eq(withdrawAmount)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST para retirar dinero
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/accounts/withdraw/{accountId}")
                        .queryParam("amount", withdrawAmount)
                        .build(accountId))  // Agregar el parámetro `amount` en la URL
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isOk()  // Verifica que el estado HTTP sea 200 (OK)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertTrue(response.isSuccess());  // Verifica que 'success' sea verdadero
                    assertEquals("Withdrawal successful.", response.getMessage());  // Verifica que el mensaje sea correcto
                    assertNull(response.getData());  // Verifica que 'data' sea nulo en una operación exitosa
                });

        verify(bankAccountService, times(1)).withdrawMoney(eq(accountId), eq(withdrawAmount));  // Verifica que el servicio fue llamado una vez
    }


    @Test
    void testWithdrawMoney_BadRequest() {
        // Arrange: Definir un ID de cuenta y un monto negativo
        UUID accountId = UUID.randomUUID();  // ID simulado de la cuenta
        double withdrawAmount = -100.0;  // Monto negativo (retiro no válido)

        // Crear una respuesta de error que simule el fallo en el retiro
        BankAccountResponseDTO mockResponse = BankAccountResponseDTO.buildError("Amount must be positive.");

        // Simular que el servicio 'withdrawMoney' devuelve la respuesta de error
        when(bankAccountService.withdrawMoney(eq(accountId), eq(withdrawAmount)))
                .thenReturn(Mono.just(mockResponse));

        // Act: Enviar la solicitud POST para retirar dinero
        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/accounts/withdraw/{accountId}")
                        .queryParam("amount", withdrawAmount)
                        .build(accountId))  // Agregar el parámetro `amount` en la URL
                .exchange();

        // Assert: Validación de los resultados
        responseSpec.expectStatus().isBadRequest()  // Verifica que el estado HTTP sea 400 (BAD REQUEST)
                .expectBody(BankAccountResponseDTO.class)  // Verifica que la respuesta sea del tipo esperado
                .value(response -> {
                    assertFalse(response.isSuccess());  // Verifica que 'success' sea falso
                    assertEquals("Amount must be positive.", response.getMessage());  // Verifica que el mensaje sea el esperado
                    assertNull(response.getData());  // Verifica que 'data' sea nulo cuando ocurre un error
                });

        verify(bankAccountService, times(1)).withdrawMoney(eq(accountId), eq(withdrawAmount));  // Verifica que el servicio fue llamado una vez
    }
}