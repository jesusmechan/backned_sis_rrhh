package pe.andina.rrhh.application.port.in;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.andina.rrhh.application.dto.AppDtos.MenuGrupoResponse;
import pe.andina.rrhh.application.dto.AppDtos.MenuItemResponse;
import pe.andina.rrhh.application.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.Usuario;

public interface SesionUseCase {
    SesionResponse actual();
    SesionResponse desde(Usuario usuario);
}
