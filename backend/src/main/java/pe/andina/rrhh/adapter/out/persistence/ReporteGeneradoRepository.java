package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ReporteGeneradoPort;
import pe.andina.rrhh.domain.model.ReporteGenerado;
public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Integer>, ReporteGeneradoPort {}
