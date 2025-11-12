package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.config.MyRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@EnableFeignClients
@ImportRuntimeHints(MyRuntimeHints.class)
public class HydroBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HydroBackendApplication.class, args);
    }
}
