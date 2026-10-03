package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.CatalogoTipo;

public interface CatalogoTipoPort {
    Optional<CatalogoTipo> findById(String codigo);
    List<CatalogoTipo> findAll();
}
