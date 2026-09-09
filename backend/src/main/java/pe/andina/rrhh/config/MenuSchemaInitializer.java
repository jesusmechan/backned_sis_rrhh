package pe.andina.rrhh.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class MenuSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public MenuSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE modalidad_contrato AS ENUM ('COLABORADOR', 'PRACTICANTE');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """);
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE estado_contrato AS ENUM ('VIGENTE', 'FINALIZADO', 'ANULADO');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(true);
        populator.addScript(new ClassPathResource("db/menu.sql"));
        populator.addScript(new ClassPathResource("db/contratos.sql"));
        populator.execute(jdbcTemplate.getDataSource());
    }
}
