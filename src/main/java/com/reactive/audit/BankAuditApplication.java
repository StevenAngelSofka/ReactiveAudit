package com.reactive.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class BankAuditApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankAuditApplication.class, args);
	}

}
