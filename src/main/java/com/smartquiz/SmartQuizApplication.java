package com.smartquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
public class SmartQuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartQuizApplication.class, args);
    }
}
