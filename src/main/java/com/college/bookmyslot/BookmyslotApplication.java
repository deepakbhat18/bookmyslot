package com.college.bookmyslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class BookmyslotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookmyslotApplication.class, args);
	}

}
