package com.example.vote_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄링 설정
 * - TaskScheduler Bean 설정
 * - 정확한 시간에 투표 마감 처리
 */
@Configuration
public class SchedulingConfig {

    /**
     * TaskScheduler Bean 설정
     * - 정확한 시간에 실행되는 작업을 위한 스케줄러
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 스레드 풀 크기
        scheduler.setThreadNamePrefix("vote-expiration-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}
