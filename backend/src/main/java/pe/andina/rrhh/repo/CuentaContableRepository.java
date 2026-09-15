package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.CuentaContable;

import java.util.Optional;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Integer> {
    Optional<CuentaContable> findByUsoIgnoreCase(String uso);
    Optional<CuentaContable> findByCodigoIgnoreCase(String codigo);
}
