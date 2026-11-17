package pe.andina.rrhh.application.port.in;

import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoArchivo;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoResponse;

import java.util.List;

/**
 * Gestión de evidencias/sustentos adjuntos a solicitudes de permiso.
 */
public interface AdjuntoUseCase {

    List<AdjuntoResponse> listarDePermiso(Integer idSolicitudPermiso);

    AdjuntoResponse subirAPermiso(Integer idSolicitudPermiso, MultipartFile archivo, Integer idPaso);

    AdjuntoArchivo descargarDePermiso(Integer idSolicitudPermiso, Integer idAdjunto);

    void eliminarDePermiso(Integer idSolicitudPermiso, Integer idAdjunto);
}
