package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionResponse;
import pe.andina.rrhh.domain.model.Area;
import pe.andina.rrhh.domain.model.Convocatoria;
import pe.andina.rrhh.domain.model.Postulacion;
import pe.andina.rrhh.domain.model.enums.EstadoConvocatoria;
import pe.andina.rrhh.domain.model.enums.EstadoPostulacion;

public interface ReclutamientoUseCase {
    List<ConvocatoriaResponse> listar();
    ConvocatoriaResponse obtener(Integer id);
    ConvocatoriaResponse crear(ConvocatoriaRequest request);
    ConvocatoriaResponse actualizar(Integer id, ConvocatoriaRequest request);
    List<PostulacionResponse> postulaciones(Integer idConvocatoria);
    PostulacionResponse registrarPostulacion(Integer idConvocatoria, PostulacionRequest request);
    PostulacionResponse actualizarPostulacion(Integer id, PostulacionRequest request);
}
