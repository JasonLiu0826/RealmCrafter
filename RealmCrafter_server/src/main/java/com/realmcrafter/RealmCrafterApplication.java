package com.realmcrafter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealmCrafterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealmCrafterApplication.class, args);
    }
}
