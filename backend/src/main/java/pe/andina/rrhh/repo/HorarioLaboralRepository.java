package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.HorarioLaboral;
import java.util.Optional;
public interface HorarioLaboralRepository extends JpaRepository<HorarioLaboral, Integer> {
    Optional<HorarioLaboral> findByNombre(String nombre);
}
