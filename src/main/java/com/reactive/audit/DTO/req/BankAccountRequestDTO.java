package com.reactive.audit.DTO.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountRequestDTO {

    @NotBlank(message = "The account number cannot be empty.")
    @Size(min = 10, max = 20, message = "The account number must be between 10 and 20 characters.")
    private String numberAccount;

    @PositiveOrZero(message = "The balance must be zero or positive.")
    private double balance;

    @NotBlank(message = "The account type cannot be empty.")
    @Pattern(regexp = "SAVINGS|CHECKING", message = "The account type must be SAVINGS or CHECKING.")
    private String type;
}
