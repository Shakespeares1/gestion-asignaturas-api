package com.gestionacademica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.gestionacademica.infrastructure.config.RenderDatabaseUrl;

@SpringBootApplication
public class GestionAsignaturasApiApplication {

	public static void main(String[] args) {
		String databaseUrl = System.getenv("DB_URL");
		if (databaseUrl != null && !databaseUrl.startsWith("jdbc:postgresql:")) {
			System.setProperty("DB_URL", RenderDatabaseUrl.toJdbcUrl(databaseUrl));
		}
		SpringApplication.run(GestionAsignaturasApiApplication.class, args);
	}

}
