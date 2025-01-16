package com.reactive.audit.DTO.req;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TransactionRequestDTO {

    @NotNull(message = "Account ID cannot be null.")
    private UUID accountId;

    @NotBlank(message = "The account number cannot be empty.")
    @Size(min = 10, max = 20, message = "The account number must be between 10 and 20 characters.")
    private String numberAccount;

    @NotBlank(message = "Transaction type cannot be empty.")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL", message = "Transaction type must be DEPOSIT or WITHDRAWAL.")
    private String type;

    @Positive(message = "Amount must be greater than 0.")
    private double amount;

    @NotNull(message = "Previous balance cannot be null.")
    @PositiveOrZero(message = "Previous balance must be zero or positive.")
    private double previousBalance;

    @NotNull(message = "Current balance cannot be null.")
    @PositiveOrZero(message = "Current balance must be zero or positive.")
    private double currentBalance;

}
