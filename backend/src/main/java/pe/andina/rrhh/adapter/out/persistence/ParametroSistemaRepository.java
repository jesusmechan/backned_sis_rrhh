package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ParametroSistemaPort;
import pe.andina.rrhh.domain.model.ParametroSistema;
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, String>, ParametroSistemaPort {}
