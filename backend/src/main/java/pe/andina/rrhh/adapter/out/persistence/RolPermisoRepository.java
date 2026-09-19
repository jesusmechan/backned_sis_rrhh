package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.RolPermisoPort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.model.RolPermiso;
import pe.andina.rrhh.domain.model.RolPermisoId;
import java.util.List;
public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId>, RolPermisoPort {
    @Query("select rp from RolPermiso rp join fetch rp.permiso where rp.rol.idRol = :idRol")
    List<RolPermiso> findByRolId(@Param("idRol") Integer idRol);

    void deleteByRol_IdRol(Integer idRol);
}
