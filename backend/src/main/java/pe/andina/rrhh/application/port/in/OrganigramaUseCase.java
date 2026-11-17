package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.CesarOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaAreaResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaNodoResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PromoverOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ReasignarOrganigramaRequest;

public interface OrganigramaUseCase {
    OrganigramaResponse arbol(boolean incluirInactivos);
    List<OrganigramaAreaResponse> areas(boolean incluirInactivos);
    OrganigramaNodoResponse equipo(Integer idEmpleado, boolean incluirInactivos);
    OrganigramaResponse reasignar(ReasignarOrganigramaRequest request);
    OrganigramaResponse promover(PromoverOrganigramaRequest request);
    OrganigramaResponse cesar(CesarOrganigramaRequest request);
}
