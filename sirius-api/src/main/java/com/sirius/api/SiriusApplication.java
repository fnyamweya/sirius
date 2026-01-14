package com.sirius.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sirius")
@EnableJpaRepositories(basePackages = "com.sirius.data.repository")
@EntityScan(basePackages = "com.sirius.data.entity")
public class SiriusApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SiriusApplication.class, args);
    }
}
