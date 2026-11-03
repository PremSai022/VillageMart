package com.villagemart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VillageMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(VillageMartApplication.class, args);
    }
}
