package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.ConfiguracionAprobacion;
import java.util.Optional;
public interface ConfiguracionAprobacionRepository extends JpaRepository<ConfiguracionAprobacion, Integer> {
    Optional<ConfiguracionAprobacion> findByCodigo(String codigo);
}
