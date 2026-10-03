package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;

public interface TramoRentaQuintaPort {
    Optional<TramoRentaQuinta> findById(Integer id);
    Optional<TramoRentaQuinta> findByOrden(Integer orden);
    List<TramoRentaQuinta> findAllByOrderByOrdenAsc();
    TramoRentaQuinta save(TramoRentaQuinta entity);
}
