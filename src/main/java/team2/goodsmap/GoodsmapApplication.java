package team2.goodsmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GoodsmapApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoodsmapApplication.class, args);
	}

}
