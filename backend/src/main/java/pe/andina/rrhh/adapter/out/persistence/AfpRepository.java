package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.AfpPort;
import pe.andina.rrhh.domain.model.Afp;
public interface AfpRepository extends JpaRepository<Afp, String>, AfpPort {}
