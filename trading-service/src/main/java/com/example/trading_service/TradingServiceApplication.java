package com.example.trading_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
	"com.example.trading_service",
	"com.example.module_common"
})
@EnableScheduling
@Slf4j
@EnableFeignClients
public class TradingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner testRunner() {
		return args -> {
			log.info("✅ TradingServiceApplication started successfully!");
			log.info("🚀 WebSocket 서버가 준비되었습니다. /ws 엔드포인트로 연결 가능합니다.");
		};
	}
}
