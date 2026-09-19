package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Auditoria;

public interface AuditoriaPort {
    Optional<Auditoria> findById(Long id);
    List<Auditoria> findAll();
    Auditoria save(Auditoria entity);
    void deleteById(Long id);
    void delete(Auditoria entity);
    boolean existsById(Long id);
    long count();

    List<Auditoria> findTop200ByOrderByIdAuditoriaDesc();
}
