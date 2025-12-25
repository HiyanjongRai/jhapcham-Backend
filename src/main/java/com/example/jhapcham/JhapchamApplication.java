package com.example.jhapcham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class JhapchamApplication {

	public static void main(String[] args) {
		SpringApplication.run(JhapchamApplication.class, args);
	}

}
