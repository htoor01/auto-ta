package io.github.htoor01.autota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AutoTaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTaApplication.class, args);
    }
}
