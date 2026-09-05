package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.RolPermiso;
import pe.andina.rrhh.domain.RolPermisoId;
import java.util.List;
public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId> {
    @Query("select rp from RolPermiso rp join fetch rp.permiso where rp.rol.idRol = :idRol")
    List<RolPermiso> findByRolId(@Param("idRol") Integer idRol);
}
