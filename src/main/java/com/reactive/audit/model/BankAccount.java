package com.reactive.audit.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document(collection = "accounts")
public class BankAccount {

    @Id
    private UUID id = UUID.randomUUID();
    private String numberAccount;
    private double balance;
    private String type;

}
