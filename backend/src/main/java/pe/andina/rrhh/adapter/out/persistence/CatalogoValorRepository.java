package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CatalogoValorPort;
import pe.andina.rrhh.domain.model.CatalogoValor;
public interface CatalogoValorRepository extends JpaRepository<CatalogoValor, Integer>, CatalogoValorPort {}
