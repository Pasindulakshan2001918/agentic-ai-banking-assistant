package com.agentic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ASYNC CONFIGURATION
 * Enables asynchronous method execution
 * Used for audit logging to avoid blocking main thread
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                  // Core number of threads
        executor.setMaxPoolSize(5);                   // Maximum number of threads
        executor.setQueueCapacity(100);               // Queue for tasks
        executor.setThreadNamePrefix("audit-");       // Thread naming for debugging
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);      // Wait 60s for tasks to complete
        executor.initialize();
        return executor;
    }
}
