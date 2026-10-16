package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.application.port.out.AdjuntoSolicitudPort;
import pe.andina.rrhh.domain.model.AdjuntoSolicitud;

import java.util.List;
import java.util.Optional;

public interface AdjuntoSolicitudRepository extends JpaRepository<AdjuntoSolicitud, Integer>, AdjuntoSolicitudPort {

    @Override
    @Query("""
            SELECT a FROM AdjuntoSolicitud a
            LEFT JOIN FETCH a.paso
            WHERE a.solicitudPermiso.idSolicitudPermiso = :id
            ORDER BY a.idAdjunto
            """)
    List<AdjuntoSolicitud> findBySolicitudPermisoId(@Param("id") Integer id);

    @Override
    Optional<AdjuntoSolicitud> findByIdAdjuntoAndSolicitudPermiso_IdSolicitudPermiso(Integer idAdjunto, Integer idSolicitud);
}