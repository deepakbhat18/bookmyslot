package com.college.bookmyslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BookmyslotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookmyslotApplication.class, args);
	}

}
