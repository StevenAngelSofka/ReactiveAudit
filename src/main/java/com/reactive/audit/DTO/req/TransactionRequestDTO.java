package com.reactive.audit.DTO.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TransactionRequestDTO {

    @NotNull(message = "Account ID cannot be null.")
    private UUID accountId;

    @NotBlank(message = "Transaction type cannot be empty.")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL", message = "Transaction type must be DEPOSIT or WITHDRAWAL.")
    private String type;

    @Positive(message = "Amount must be greater than 0.")
    private double amount;

    @NotNull(message = "Balance before cannot be null.")
    @Positive(message = "Balance before must be greater than 0.")
    private double balanceBefore;

    @NotNull(message = "Balance after cannot be null.")
    @Positive(message = "Balance after must be greater than 0.")
    private double balanceAfter;

}
