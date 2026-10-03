package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.RegimenLaboralPort;
import pe.andina.rrhh.domain.model.RegimenLaboral;
public interface RegimenLaboralRepository extends JpaRepository<RegimenLaboral, String>, RegimenLaboralPort {}
