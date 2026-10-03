package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.CatalogoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.application.dto.AppDtos.AfpItem;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorItem;
import pe.andina.rrhh.application.dto.AppDtos.IdNombre;
import pe.andina.rrhh.application.dto.AppDtos.PermisoFuncionalItem;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralItem;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoItem;
import pe.andina.rrhh.application.port.out.AfpPort;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.application.port.out.CatalogoValorPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;
import pe.andina.rrhh.application.port.out.ParametroSistemaPort;
import pe.andina.rrhh.application.port.out.PermisoFuncionalPort;
import pe.andina.rrhh.application.port.out.RegimenLaboralPort;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;
import pe.andina.rrhh.domain.model.ParametroSistema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CatalogoService implements CatalogoUseCase {

    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final TipoPermisoPort tipoPermisoRepository;
    private final RolPort rolRepository;
    private final PermisoFuncionalPort permisoFuncionalRepository;
    private final ParametroSistemaPort parametroRepository;
    private final CatalogoValorPort valorRepository;
    private final AfpPort afpRepository;
    private final RegimenLaboralPort regimenRepository;

    public CatalogoService(AreaPort areaRepository,
                           CargoPort cargoRepository,
                           HorarioLaboralPort horarioRepository,
                           TipoPermisoPort tipoPermisoRepository,
                           RolPort rolRepository,
                           PermisoFuncionalPort permisoFuncionalRepository,
                           ParametroSistemaPort parametroRepository,
                           CatalogoValorPort valorRepository,
                           AfpPort afpRepository,
                           RegimenLaboralPort regimenRepository) {
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.rolRepository = rolRepository;
        this.permisoFuncionalRepository = permisoFuncionalRepository;
        this.parametroRepository = parametroRepository;
        this.valorRepository = valorRepository;
        this.afpRepository = afpRepository;
        this.regimenRepository = regimenRepository;
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
    public List<TipoPermisoItem> tiposPermiso() {
        return tipoPermisoRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .map(t -> new TipoPermisoItem(t.getIdTipoPermiso(), t.getCodigo(), t.getNombre(),
                        Boolean.TRUE.equals(t.getEsVacaciones())))
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

    /** Parámetros visibles en sesión; los de ámbito privado no salen del servidor. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> parametros() {
        return parametroRepository.findAll().stream()
                .filter(p -> !ParametroSistema.AMBITO_PRIVADO.equals(p.getAmbito()))
                .map(p -> Map.of("clave", p.getClave(), "valor", p.getValor(), "descripcion", p.getDescripcion() != null ? p.getDescripcion() : ""))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> configuracionPublica() {
        Map<String, String> config = new TreeMap<>();
        parametroRepository.findAll().stream()
                .filter(p -> ParametroSistema.AMBITO_PUBLICO.equals(p.getAmbito()))
                .forEach(p -> config.put(p.getClave(), p.getValor()));
        return config;
    }

    @Transactional(readOnly = true)
    public Map<String, List<CatalogoValorItem>> valores() {
        Map<String, List<CatalogoValorItem>> porTipo = new LinkedHashMap<>();
        valorRepository.findAllByOrderByTipoAscOrdenAscNombreAsc().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .forEach(v -> porTipo.computeIfAbsent(v.getTipo(), k -> new java.util.ArrayList<>())
                        .add(new CatalogoValorItem(v.getCodigo(), v.getNombre(), v.getTono(), v.getOrden(),
                                Boolean.TRUE.equals(v.getPorDefecto()), v.getRegla(), v.getMensajeRegla())));
        return porTipo;
    }

    @Transactional(readOnly = true)
    public List<AfpItem> afps() {
        return afpRepository.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .map(a -> new AfpItem(a.getCodigo(), a.getNombre(), a.getTasaComision()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegimenLaboralItem> regimenesLaborales() {
        return regimenRepository.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .map(r -> new RegimenLaboralItem(r.getCodigo(), r.getNombre(), r.getDescripcion(),
                        Boolean.TRUE.equals(r.getPorDefecto())))
                .toList();
    }
}
