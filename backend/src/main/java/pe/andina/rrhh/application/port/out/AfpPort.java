package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Afp;

public interface AfpPort {
    Optional<Afp> findById(String codigo);
    List<Afp> findAllByOrderByOrdenAscNombreAsc();
    Afp save(Afp entity);
    boolean existsById(String codigo);
}
