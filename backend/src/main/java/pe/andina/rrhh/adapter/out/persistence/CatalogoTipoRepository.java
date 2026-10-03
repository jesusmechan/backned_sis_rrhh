package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CatalogoTipoPort;
import pe.andina.rrhh.domain.model.CatalogoTipo;
public interface CatalogoTipoRepository extends JpaRepository<CatalogoTipo, String>, CatalogoTipoPort {}
