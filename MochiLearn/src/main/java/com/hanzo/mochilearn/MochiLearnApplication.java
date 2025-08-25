package com.hanzo.mochilearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing

public class MochiLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(MochiLearnApplication.class, args);
    }

}
