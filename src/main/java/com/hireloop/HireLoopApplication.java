package com.hireloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HireLoopApplication {
    public static void main(String[] args) {
        SpringApplication.run(HireLoopApplication.class, args);
    }
}
