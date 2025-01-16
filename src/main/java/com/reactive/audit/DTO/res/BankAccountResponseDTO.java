package com.reactive.audit.DTO.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountResponseDTO {
    private boolean success;
    private String message;
    private Object data;

    public static BankAccountResponseDTO buildSuccess(String message, Object data) {
        return BankAccountResponseDTO.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static BankAccountResponseDTO buildError(String message) {
        return BankAccountResponseDTO.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
