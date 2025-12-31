package com.college.bookmyslot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;



@EnableScheduling
@EnableAsync
@EnableRetry
@SpringBootApplication
public class BookmyslotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookmyslotApplication.class, args);
	}

}
