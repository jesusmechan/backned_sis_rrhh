package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.CatalogoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.application.dto.AppDtos.IdNombre;
import pe.andina.rrhh.application.dto.AppDtos.PermisoFuncionalItem;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;
import pe.andina.rrhh.application.port.out.ParametroSistemaPort;
import pe.andina.rrhh.application.port.out.PermisoFuncionalPort;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;

import java.util.List;
import java.util.Map;

@Service
public class CatalogoService implements CatalogoUseCase {

    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final TipoPermisoPort tipoPermisoRepository;
    private final RolPort rolRepository;
    private final PermisoFuncionalPort permisoFuncionalRepository;
    private final ParametroSistemaPort parametroRepository;

    public CatalogoService(AreaPort areaRepository,
                           CargoPort cargoRepository,
                           HorarioLaboralPort horarioRepository,
                           TipoPermisoPort tipoPermisoRepository,
                           RolPort rolRepository,
                           PermisoFuncionalPort permisoFuncionalRepository,
                           ParametroSistemaPort parametroRepository) {
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.rolRepository = rolRepository;
        this.permisoFuncionalRepository = permisoFuncionalRepository;
        this.parametroRepository = parametroRepository;
    }

    @Transactional(readOnly = true)
    public List<IdNombre> areas() {
        return areaRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .map(a -> new IdNombre(a.getIdArea(), a.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IdNombre> cargos() {
        return cargoRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .map(c -> new IdNombre(c.getIdCargo(), c.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IdNombre> horarios() {
        return horarioRepository.findAll().stream()
                .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                .map(h -> new IdNombre(h.getIdHorario(),
                        h.getNombre() + " · " + h.getHoraIngreso() + "–" + h.getHoraSalida()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItem> tiposPermiso() {
        return tipoPermisoRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .map(t -> new CatalogoItem(t.getIdTipoPermiso(), t.getCodigo(), t.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItem> roles() {
        return rolRepository.findAllByOrderByNombreAsc().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .map(r -> new CatalogoItem(r.getIdRol(), r.getCodigo(), r.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoFuncionalItem> permisosFuncionales() {
        return permisoFuncionalRepository.findAll().stream()
                .map(p -> new PermisoFuncionalItem(p.getIdPermiso(), p.getCodigo(), p.getNombre(), p.getModulo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> parametros() {
        return parametroRepository.findAll().stream()
                .map(p -> Map.of("clave", p.getClave(), "valor", p.getValor(), "descripcion", p.getDescripcion() != null ? p.getDescripcion() : ""))
                .toList();
    }
}
