package com.gymflow.gym;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GymServiceApplication.class, args);
    }
}

