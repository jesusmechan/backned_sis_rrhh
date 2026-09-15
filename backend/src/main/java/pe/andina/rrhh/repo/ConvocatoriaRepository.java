package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Convocatoria;

import java.util.List;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {
    List<Convocatoria> findAllByOrderByIdConvocatoriaDesc();
}
