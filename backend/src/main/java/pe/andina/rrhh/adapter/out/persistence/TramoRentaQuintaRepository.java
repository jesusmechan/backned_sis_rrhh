package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.TramoRentaQuintaPort;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;
public interface TramoRentaQuintaRepository extends JpaRepository<TramoRentaQuinta, Integer>, TramoRentaQuintaPort {}
