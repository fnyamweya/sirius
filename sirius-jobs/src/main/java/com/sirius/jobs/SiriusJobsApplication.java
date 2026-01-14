package com.sirius.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sirius")
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sirius.data.repository")
@EntityScan(basePackages = "com.sirius.data.entity")
public class SiriusJobsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiriusJobsApplication.class, args);
    }
}
