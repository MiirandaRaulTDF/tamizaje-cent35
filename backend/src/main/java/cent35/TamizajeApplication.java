package cent35;

import cent35.repositories.PacienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TamizajeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TamizajeApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(PacienteRepository pacienteRepository) {
		return (args) -> {

		};
	}
}