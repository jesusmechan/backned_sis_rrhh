package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CuentaContablePort;
import pe.andina.rrhh.domain.model.CuentaContable;

import java.util.Optional;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Integer>, CuentaContablePort {
    Optional<CuentaContable> findByUsoIgnoreCase(String uso);
    Optional<CuentaContable> findByCodigoIgnoreCase(String codigo);
}
