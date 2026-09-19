package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.PermisoFuncionalPort;
import pe.andina.rrhh.domain.model.PermisoFuncional;
public interface PermisoFuncionalRepository extends JpaRepository<PermisoFuncional, Integer>, PermisoFuncionalPort {}
