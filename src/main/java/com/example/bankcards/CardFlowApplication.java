package com.example.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CardFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardFlowApplication.class, args);
    }
}