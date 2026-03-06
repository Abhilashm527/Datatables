package com.dataflow.DataTable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class DataTableApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataTableApplication.class, args);
	}

}
