package bashkirov.store_original;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StoreOriginalApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoreOriginalApplication.class, args);
	}

}
