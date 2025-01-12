package com.devita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevitaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevitaApplication.class, args);
	}

}
