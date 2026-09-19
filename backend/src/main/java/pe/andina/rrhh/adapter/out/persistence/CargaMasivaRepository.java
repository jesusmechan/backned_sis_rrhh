package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CargaMasivaPort;
import pe.andina.rrhh.domain.model.CargaMasiva;
public interface CargaMasivaRepository extends JpaRepository<CargaMasiva, Integer>, CargaMasivaPort {}
