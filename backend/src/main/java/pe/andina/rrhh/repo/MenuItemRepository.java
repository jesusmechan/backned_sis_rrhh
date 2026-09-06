package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.MenuItem;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    @Query("""
            select distinct m from MenuItem m
            join m.perfiles p
            where p.idRol = :idRol and m.activo = true
            order by m.orden
            """)
    List<MenuItem> findActivosByPerfil(@Param("idRol") Integer idRol);

    @Query("select distinct m from MenuItem m left join fetch m.perfiles order by m.orden, m.idMenu")
    List<MenuItem> findAllWithPerfiles();

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdMenuNot(String codigo, Integer idMenu);
}
