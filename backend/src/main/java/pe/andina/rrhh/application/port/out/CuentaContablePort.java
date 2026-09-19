package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.CuentaContable;

public interface CuentaContablePort {
    Optional<CuentaContable> findById(Integer id);
    List<CuentaContable> findAll();
    CuentaContable save(CuentaContable entity);
    void deleteById(Integer id);
    void delete(CuentaContable entity);
    boolean existsById(Integer id);
    long count();

    Optional<CuentaContable> findByUsoIgnoreCase(String uso);
    Optional<CuentaContable> findByCodigoIgnoreCase(String codigo);
}
