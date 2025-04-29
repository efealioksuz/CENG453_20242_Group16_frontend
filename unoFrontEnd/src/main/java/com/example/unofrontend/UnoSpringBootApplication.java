package com.example.unofrontend;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UnoSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnoSpringBootApplication.class, args);
        Application.launch(UnoApplication.class, args);
    }
} 