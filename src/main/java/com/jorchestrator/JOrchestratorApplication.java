// src/main/java/com/jorchestrator/JOrchestratorApplication.java
package com.jorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(JOrchestratorApplication.class, args);
    }
}