package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ConvocatoriaPort;
import pe.andina.rrhh.domain.model.Convocatoria;

import java.util.List;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer>, ConvocatoriaPort {
    List<Convocatoria> findAllByOrderByIdConvocatoriaDesc();
}
