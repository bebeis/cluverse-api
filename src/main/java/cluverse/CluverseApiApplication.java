package cluverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.ZoneId;
import java.util.TimeZone;

@EnableJpaAuditing
@SpringBootApplication
public class CluverseApiApplication {

    private static final ZoneId APPLICATION_ZONE_ID = ZoneId.of("Asia/Seoul");

    static {
        configureDefaultTimeZone();
    }

    static void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_ZONE_ID));
    }

    public static void main(String[] args) {
        SpringApplication.run(CluverseApiApplication.class, args);
    }

}
