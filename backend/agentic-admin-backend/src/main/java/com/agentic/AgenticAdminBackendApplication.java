package com.agentic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgenticAdminBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgenticAdminBackendApplication.class, args);
	}

}
