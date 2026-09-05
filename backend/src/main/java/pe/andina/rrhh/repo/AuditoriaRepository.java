package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Auditoria;
import java.util.List;
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findTop200ByOrderByIdAuditoriaDesc();
}
