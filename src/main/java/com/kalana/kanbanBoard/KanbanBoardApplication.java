package com.kalana.kanbanBoard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KanbanBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(KanbanBoardApplication.class, args);
	}

}
