package com.example.vote_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄러 활성화
public class VoteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoteServiceApplication.class, args);
	}

}
  	   