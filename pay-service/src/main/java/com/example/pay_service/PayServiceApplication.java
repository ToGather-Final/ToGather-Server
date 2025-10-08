package com.example.pay_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.pay_service.domain")
@EnableJpaRepositories("com.example.pay_service.repository")
public class PayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayServiceApplication.class, args);
	}

}
