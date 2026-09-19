package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.AuditoriaPort;
import pe.andina.rrhh.domain.model.Auditoria;
import java.util.List;
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, AuditoriaPort {
    List<Auditoria> findTop200ByOrderByIdAuditoriaDesc();
}
