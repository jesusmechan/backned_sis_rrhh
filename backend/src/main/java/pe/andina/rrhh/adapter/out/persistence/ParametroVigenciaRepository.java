package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ParametroVigenciaPort;
import pe.andina.rrhh.domain.model.ParametroVigencia;
public interface ParametroVigenciaRepository extends JpaRepository<ParametroVigencia, Integer>, ParametroVigenciaPort {}
