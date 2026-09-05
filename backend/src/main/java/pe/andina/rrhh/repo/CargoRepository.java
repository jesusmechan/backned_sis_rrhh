package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Cargo;
public interface CargoRepository extends JpaRepository<Cargo, Integer> {}
