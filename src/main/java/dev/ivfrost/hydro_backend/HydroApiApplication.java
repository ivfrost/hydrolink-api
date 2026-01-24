package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.config.MyRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@EnableCaching
@ImportRuntimeHints(MyRuntimeHints.class)
public class HydroApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(HydroApiApplication.class, args);
  }

}
