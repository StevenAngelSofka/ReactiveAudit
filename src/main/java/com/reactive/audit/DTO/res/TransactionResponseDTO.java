package com.reactive.audit.DTO.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
    private boolean success;
    private String message;
    private Object data;

    public static TransactionResponseDTO buildSuccess(String message, Object data) {
        return TransactionResponseDTO.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static TransactionResponseDTO buildError(String message) {
        return TransactionResponseDTO.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
