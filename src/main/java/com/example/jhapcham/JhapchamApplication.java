package com.example.jhapcham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@EnableAsync
public class JhapchamApplication {

	public static void main(String[] args) {
		SpringApplication.run(JhapchamApplication.class, args);
	}

}
