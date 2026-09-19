package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.application.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;

public interface SolicitudUseCase {
    PermisoResponse crearPermiso(PermisoRequest request);
    HoraExtraResponse crearHoraExtra(HoraExtraRequest request);
    List<PermisoResponse> listarPermisos();
    PermisoResponse obtenerPermiso(Integer id);
    List<HoraExtraResponse> listarHorasExtras();
    HoraExtraResponse obtenerHoraExtra(Integer id);
    PermisoResponse cancelarPermiso(Integer id);
    HoraExtraResponse cancelarHoraExtra(Integer id);
    BandejaItem pasoPendiente(Integer idPaso);
    PasoResponse decidir(Integer idPaso, boolean aprobar, DecisionRequest request);
    List<BandejaItem> bandeja(String vista);
    List<HistorialResponse> historialPermiso(Integer id);
    List<HistorialResponse> historialHoraExtra(Integer id);
}
