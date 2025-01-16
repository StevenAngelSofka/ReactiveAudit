package com.reactive.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Document(collection = "transactions")
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    private UUID id = UUID.randomUUID();
    private UUID accountId;
    private String numberAccount;
    private String type;
    private double amount;
    private double previousBalance;
    private double currentBalance;
    private LocalDateTime transactionDate;

}
