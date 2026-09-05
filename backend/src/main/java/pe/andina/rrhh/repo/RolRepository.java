package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Rol;
import java.util.Optional;
public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByCodigo(String codigo);
}
