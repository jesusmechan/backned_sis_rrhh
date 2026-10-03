package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.AfpRequest;
import pe.andina.rrhh.application.dto.AppDtos.AfpResponse;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoTipoResponse;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorRequest;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorResponse;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralRequest;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralResponse;
import pe.andina.rrhh.application.dto.AppDtos.TramoQuintaRequest;
import pe.andina.rrhh.application.dto.AppDtos.TramoQuintaResponse;
import pe.andina.rrhh.application.dto.AppDtos.VigenciaRequest;
import pe.andina.rrhh.application.dto.AppDtos.VigenciaResponse;

/** Mantenedores de catálogos y tablas de reglas de planilla. */
public interface ReglasMaestroUseCase {
    List<CatalogoTipoResponse> tiposCatalogo();
    List<CatalogoValorResponse> valoresCatalogo();
    CatalogoValorResponse crearValor(CatalogoValorRequest r);
    CatalogoValorResponse actualizarValor(Integer id, CatalogoValorRequest r);

    List<AfpResponse> afps();
    AfpResponse crearAfp(AfpRequest r);
    AfpResponse actualizarAfp(String codigo, AfpRequest r);

    List<RegimenLaboralResponse> regimenes();
    RegimenLaboralResponse crearRegimen(RegimenLaboralRequest r);
    RegimenLaboralResponse actualizarRegimen(String codigo, RegimenLaboralRequest r);

    List<TramoQuintaResponse> tramos();
    TramoQuintaResponse crearTramo(TramoQuintaRequest r);
    TramoQuintaResponse actualizarTramo(Integer id, TramoQuintaRequest r);

    List<VigenciaResponse> vigencias();
    VigenciaResponse crearVigencia(VigenciaRequest r);
    VigenciaResponse actualizarVigencia(Integer id, VigenciaRequest r);
    void eliminarVigencia(Integer id);
}
