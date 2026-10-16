package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionPort;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.model.enums.TipoOrigenFlujo;

import java.util.Optional;

public interface ConfiguracionAprobacionRepository extends JpaRepository<ConfiguracionAprobacion, Integer>, ConfiguracionAprobacionPort {
    Optional<ConfiguracionAprobacion> findByCodigo(String codigo);

    @Override
    @Query("""
            SELECT c FROM ConfiguracionAprobacion c
            WHERE c.activo = true
              AND c.tipoOrigen = :tipo
              AND c.tipoPermiso.idTipoPermiso = :idTipoPermiso
            """)
    Optional<ConfiguracionAprobacion> findActivaPorTipoPermiso(@Param("tipo") TipoOrigenFlujo tipo,
                                                                @Param("idTipoPermiso") Integer idTipoPermiso);

    @Override
    @Query("""
            SELECT c FROM ConfiguracionAprobacion c
            WHERE c.activo = true
              AND c.tipoOrigen = :tipo
              AND c.tipoPermiso IS NULL
            """)
    Optional<ConfiguracionAprobacion> findActivaGenerica(@Param("tipo") TipoOrigenFlujo tipo);
}
