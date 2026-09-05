package pe.andina.rrhh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pe.andina.rrhh.config.EnvFileLoader;

@SpringBootApplication
public class RrhhApplication {

    public static void main(String[] args) {
        EnvFileLoader.load();
        SpringApplication.run(RrhhApplication.class, args);
    }
}
