package com.innercircle.project_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ProjectOneApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectOneApplication.class, args);
	}

}
