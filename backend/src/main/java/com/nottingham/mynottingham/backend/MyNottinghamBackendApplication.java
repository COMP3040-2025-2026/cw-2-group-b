package com.nottingham.mynottingham.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MyNottinghamBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyNottinghamBackendApplication.class, args);
        System.out.println("\n==============================================");
        System.out.println("MyNottingham Backend API is running!");
        System.out.println("API Base URL: http://localhost:8080/api");
        System.out.println("H2 Console (dev): http://localhost:8080/api/h2-console");
        System.out.println("==============================================\n");
    }
}
