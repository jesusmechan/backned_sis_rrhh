package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.dto.AppDtos.IdNombre;
import pe.andina.rrhh.repo.AreaRepository;
import pe.andina.rrhh.repo.CargoRepository;
import pe.andina.rrhh.repo.HorarioLaboralRepository;
import pe.andina.rrhh.repo.ParametroSistemaRepository;
import pe.andina.rrhh.repo.PermisoFuncionalRepository;
import pe.andina.rrhh.repo.RolRepository;
import pe.andina.rrhh.repo.TipoPermisoRepository;

import java.util.List;
import java.util.Map;

@Service
public class CatalogoService {

    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final HorarioLaboralRepository horarioRepository;
    private final TipoPermisoRepository tipoPermisoRepository;
    private final RolRepository rolRepository;
    private final PermisoFuncionalRepository permisoFuncionalRepository;
    private final ParametroSistemaRepository parametroRepository;

    public CatalogoService(AreaRepository areaRepository,
                           CargoRepository cargoRepository,
                           HorarioLaboralRepository horarioRepository,
                           TipoPermisoRepository tipoPermisoRepository,
                           RolRepository rolRepository,
                           PermisoFuncionalRepository permisoFuncionalRepository,
                           ParametroSistemaRepository parametroRepository) {
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
        return areaRepository.findAll().stream().map(a -> new IdNombre(a.getIdArea(), a.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<IdNombre> cargos() {
        return cargoRepository.findAll().stream().map(c -> new IdNombre(c.getIdCargo(), c.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<IdNombre> horarios() {
        return horarioRepository.findAll().stream()
                .map(h -> new IdNombre(h.getIdHorario(),
                        h.getNombre() + " · " + h.getHoraIngreso() + "–" + h.getHoraSalida()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItem> tiposPermiso() {
        return tipoPermisoRepository.findAll().stream()
                .map(t -> new CatalogoItem(t.getIdTipoPermiso(), t.getCodigo(), t.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItem> roles() {
        return rolRepository.findAll().stream()
                .map(r -> new CatalogoItem(r.getIdRol(), r.getCodigo(), r.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItem> permisosFuncionales() {
        return permisoFuncionalRepository.findAll().stream()
                .map(p -> new CatalogoItem(p.getIdPermiso(), p.getCodigo(), p.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> parametros() {
        return parametroRepository.findAll().stream()
                .map(p -> Map.of("clave", p.getClave(), "valor", p.getValor(), "descripcion", p.getDescripcion() != null ? p.getDescripcion() : ""))
                .toList();
    }
}
