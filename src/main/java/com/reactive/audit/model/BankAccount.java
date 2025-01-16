package com.reactive.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document(collection = "accounts")
@AllArgsConstructor
@NoArgsConstructor
public class BankAccount {

    @Id
    private UUID id = UUID.randomUUID();
    private String numberAccount;
    private double balance;
    private String type;

}
