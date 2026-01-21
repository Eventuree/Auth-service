package org.eventure.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.TimeZone;

@SpringBootApplication
@EnableWebSecurity
public class AuthServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
