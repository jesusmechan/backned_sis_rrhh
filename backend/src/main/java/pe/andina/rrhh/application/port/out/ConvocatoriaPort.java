package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Convocatoria;

public interface ConvocatoriaPort {
    Optional<Convocatoria> findById(Integer id);
    List<Convocatoria> findAll();
    Convocatoria save(Convocatoria entity);
    void deleteById(Integer id);
    void delete(Convocatoria entity);
    boolean existsById(Integer id);
    long count();

    List<Convocatoria> findAllByOrderByIdConvocatoriaDesc();
}
