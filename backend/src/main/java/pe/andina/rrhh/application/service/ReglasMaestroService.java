package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.ReglasMaestroUseCase;
import pe.andina.rrhh.application.port.out.AfpPort;
import pe.andina.rrhh.application.port.out.CatalogoTipoPort;
import pe.andina.rrhh.application.port.out.CatalogoValorPort;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.ParametroVigenciaPort;
import pe.andina.rrhh.application.port.out.RegimenLaboralPort;
import pe.andina.rrhh.application.port.out.TramoRentaQuintaPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Afp;
import pe.andina.rrhh.domain.model.CatalogoTipo;
import pe.andina.rrhh.domain.model.CatalogoValor;
import pe.andina.rrhh.domain.model.ParametroVigencia;
import pe.andina.rrhh.domain.model.RegimenLaboral;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
public class ReglasMaestroService implements ReglasMaestroUseCase {

    private final CatalogoTipoPort tipoRepository;
    private final CatalogoValorPort valorRepository;
    private final AfpPort afpRepository;
    private final RegimenLaboralPort regimenRepository;
    private final TramoRentaQuintaPort tramoRepository;
    private final ParametroVigenciaPort vigenciaRepository;
    private final ParametroPort parametros;
    private final AuditoriaUseCase auditoriaService;
    private final CurrentUserPort currentUser;

    public ReglasMaestroService(CatalogoTipoPort tipoRepository,
                                CatalogoValorPort valorRepository,
                                AfpPort afpRepository,
                                RegimenLaboralPort regimenRepository,
                                TramoRentaQuintaPort tramoRepository,
                                ParametroVigenciaPort vigenciaRepository,
                                ParametroPort parametros,
                                AuditoriaUseCase auditoriaService,
                                CurrentUserPort currentUser) {
        this.tipoRepository = tipoRepository;
        this.valorRepository = valorRepository;
        this.afpRepository = afpRepository;
        this.regimenRepository = regimenRepository;
        this.tramoRepository = tramoRepository;
        this.vigenciaRepository = vigenciaRepository;
        this.parametros = parametros;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------- catálogos

    @Transactional(readOnly = true)
    public List<CatalogoTipoResponse> tiposCatalogo() {
        return tipoRepository.findAll().stream()
                .sorted(Comparator.comparing(CatalogoTipo::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new CatalogoTipoResponse(t.getCodigo(), t.getNombre(), Boolean.TRUE.equals(t.getExtensible())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoValorResponse> valoresCatalogo() {
        Map<String, CatalogoTipo> tipos = tipoRepository.findAll().stream()
                .collect(Collectors.toMap(CatalogoTipo::getCodigo, Function.identity()));
        return valorRepository.findAllByOrderByTipoAscOrdenAscNombreAsc().stream()
                .map(v -> toValor(v, tipos.get(v.getTipo())))
                .toList();
    }

    /** Solo los catálogos extensibles admiten códigos nuevos; los demás están ligados a la lógica del sistema. */
    @Transactional
    public CatalogoValorResponse crearValor(CatalogoValorRequest r) {
        CatalogoTipo tipo = tipo(r.tipo());
        if (!Boolean.TRUE.equals(tipo.getExtensible())) {
            throw DomainException.badRequest("El catálogo «" + tipo.getNombre() + "» no admite valores nuevos; edite los existentes");
        }
        String codigo = codigo(r.codigo());
        if (valorRepository.findByTipoAndCodigo(tipo.getCodigo(), codigo).isPresent()) {
            throw DomainException.conflict("Ya existe ese código en el catálogo");
        }
        CatalogoValor v = new CatalogoValor();
        v.setTipo(tipo.getCodigo());
        v.setCodigo(codigo);
        aplicar(v, r);
        valorRepository.save(v);
        auditar("CREAR", "CATALOGO_VALOR", v.getIdValor(), v.getTipo() + "/" + v.getCodigo());
        return toValor(v, tipo);
    }

    @Transactional
    public CatalogoValorResponse actualizarValor(Integer id, CatalogoValorRequest r) {
        CatalogoValor v = valorRepository.findById(id).orElseThrow(() -> DomainException.notFound("Valor de catálogo no existe"));
        CatalogoTipo tipo = tipo(v.getTipo());
        String codigo = codigo(r.codigo());
        if (!codigo.equals(v.getCodigo())) {
            if (!Boolean.TRUE.equals(tipo.getExtensible())) {
                throw DomainException.badRequest("El código de este catálogo lo usa el sistema y no se puede cambiar");
            }
            valorRepository.findByTipoAndCodigo(v.getTipo(), codigo)
                    .ifPresent(x -> { throw DomainException.conflict("Ya existe ese código en el catálogo"); });
            v.setCodigo(codigo);
        }
        aplicar(v, r);
        auditar("ACTUALIZAR", "CATALOGO_VALOR", v.getIdValor(), v.getTipo() + "/" + v.getCodigo());
        return toValor(v, tipo);
    }

    private void aplicar(CatalogoValor v, CatalogoValorRequest r) {
        String regla = blankToNull(r.regla());
        if (regla != null) {
            try {
                Pattern.compile(regla);
            } catch (PatternSyntaxException e) {
                throw DomainException.badRequest("La regla no es una expresión regular válida");
            }
        }
        v.setNombre(trim(r.nombre()));
        v.setTono(blankToNull(r.tono()));
        v.setOrden(r.orden() != null ? r.orden() : 0);
        v.setRegla(regla);
        v.setMensajeRegla(blankToNull(r.mensajeRegla()));
        v.setActivo(r.activo() == null || r.activo());
        boolean porDefecto = Boolean.TRUE.equals(r.porDefecto());
        if (porDefecto && !Boolean.TRUE.equals(v.getActivo())) {
            throw DomainException.badRequest("El valor por defecto debe estar activo");
        }
        if (porDefecto) {
            valorRepository.findByTipo(v.getTipo()).stream()
                    .filter(o -> !o.getCodigo().equals(v.getCodigo()) && Boolean.TRUE.equals(o.getPorDefecto()))
                    .forEach(o -> o.setPorDefecto(false));
        }
        v.setPorDefecto(porDefecto);
    }

    private CatalogoTipo tipo(String codigo) {
        return tipoRepository.findById(trim(codigo)).orElseThrow(() -> DomainException.badRequest("Catálogo no existe"));
    }

    private CatalogoValorResponse toValor(CatalogoValor v, CatalogoTipo t) {
        return new CatalogoValorResponse(v.getIdValor(), v.getTipo(), t != null ? t.getNombre() : v.getTipo(),
                t != null && Boolean.TRUE.equals(t.getExtensible()), v.getCodigo(), v.getNombre(), v.getTono(),
                v.getOrden(), v.getPorDefecto(), v.getRegla(), v.getMensajeRegla(), v.getActivo());
    }

    // ---------------------------------------------------------------- AFP

    @Transactional(readOnly = true)
    public List<AfpResponse> afps() {
        return afpRepository.findAllByOrderByOrdenAscNombreAsc().stream().map(this::toAfp).toList();
    }

    @Transactional
    public AfpResponse crearAfp(AfpRequest r) {
        String codigo = codigo(r.codigo());
        if (afpRepository.existsById(codigo)) {
            throw DomainException.conflict("Ya existe una AFP con ese código");
        }
        Afp a = new Afp();
        a.setCodigo(codigo);
        aplicar(a, r);
        afpRepository.save(a);
        auditar("CREAR", "AFP", null, codigo);
        return toAfp(a);
    }

    @Transactional
    public AfpResponse actualizarAfp(String codigo, AfpRequest r) {
        Afp a = afpRepository.findById(codigo).orElseThrow(() -> DomainException.notFound("AFP no existe"));
        if (!codigo(r.codigo()).equals(a.getCodigo())) {
            throw DomainException.badRequest("El código de la AFP no se puede cambiar; los contratos lo referencian");
        }
        aplicar(a, r);
        auditar("ACTUALIZAR", "AFP", null, codigo);
        return toAfp(a);
    }

    private void aplicar(Afp a, AfpRequest r) {
        a.setNombre(trim(r.nombre()));
        a.setTasaComision(r.tasaComision());
        a.setOrden(r.orden() != null ? r.orden() : 0);
        a.setActivo(r.activo() == null || r.activo());
    }

    private AfpResponse toAfp(Afp a) {
        return new AfpResponse(a.getCodigo(), a.getNombre(), a.getTasaComision(), a.getOrden(), a.getActivo());
    }

    // ---------------------------------------------------------------- regímenes laborales

    @Transactional(readOnly = true)
    public List<RegimenLaboralResponse> regimenes() {
        return regimenRepository.findAllByOrderByOrdenAscNombreAsc().stream().map(this::toRegimen).toList();
    }

    @Transactional
    public RegimenLaboralResponse crearRegimen(RegimenLaboralRequest r) {
        String codigo = codigo(r.codigo());
        if (regimenRepository.existsById(codigo)) {
            throw DomainException.conflict("Ya existe un régimen con ese código");
        }
        RegimenLaboral g = new RegimenLaboral();
        g.setCodigo(codigo);
        aplicar(g, r);
        regimenRepository.save(g);
        auditar("CREAR", "REGIMEN_LABORAL", null, codigo);
        return toRegimen(g);
    }

    @Transactional
    public RegimenLaboralResponse actualizarRegimen(String codigo, RegimenLaboralRequest r) {
        RegimenLaboral g = regimenRepository.findById(codigo).orElseThrow(() -> DomainException.notFound("Régimen no existe"));
        if (!codigo(r.codigo()).equals(g.getCodigo())) {
            throw DomainException.badRequest("El código del régimen no se puede cambiar; los contratos lo referencian");
        }
        aplicar(g, r);
        auditar("ACTUALIZAR", "REGIMEN_LABORAL", null, codigo);
        return toRegimen(g);
    }

    private void aplicar(RegimenLaboral g, RegimenLaboralRequest r) {
        g.setNombre(trim(r.nombre()));
        g.setDescripcion(blankToNull(r.descripcion()));
        g.setFactorGratificacion(r.factorGratificacion());
        g.setFactorCts(r.factorCts());
        g.setAplicaEssalud(r.aplicaEssalud() == null || r.aplicaEssalud());
        g.setOrden(r.orden() != null ? r.orden() : 0);
        g.setActivo(r.activo() == null || r.activo());
        boolean porDefecto = Boolean.TRUE.equals(r.porDefecto());
        if (porDefecto && !Boolean.TRUE.equals(g.getActivo())) {
            throw DomainException.badRequest("El régimen por defecto debe estar activo");
        }
        if (porDefecto) {
            regimenRepository.findAllByOrderByOrdenAscNombreAsc().stream()
                    .filter(o -> !o.getCodigo().equals(g.getCodigo()) && Boolean.TRUE.equals(o.getPorDefecto()))
                    .forEach(o -> o.setPorDefecto(false));
        }
        g.setPorDefecto(porDefecto);
    }

    private RegimenLaboralResponse toRegimen(RegimenLaboral g) {
        return new RegimenLaboralResponse(g.getCodigo(), g.getNombre(), g.getDescripcion(), g.getFactorGratificacion(),
                g.getFactorCts(), g.getAplicaEssalud(), g.getPorDefecto(), g.getOrden(), g.getActivo());
    }

    // ---------------------------------------------------------------- tramos de 5ta

    @Transactional(readOnly = true)
    public List<TramoQuintaResponse> tramos() {
        return tramoRepository.findAllByOrderByOrdenAsc().stream().map(this::toTramo).toList();
    }

    @Transactional
    public TramoQuintaResponse crearTramo(TramoQuintaRequest r) {
        tramoRepository.findByOrden(r.orden())
                .ifPresent(x -> { throw DomainException.conflict("Ya existe un tramo con ese orden"); });
        TramoRentaQuinta t = new TramoRentaQuinta();
        aplicar(t, r);
        tramoRepository.save(t);
        auditar("CREAR", "TRAMO_QUINTA", t.getIdTramo(), String.valueOf(t.getOrden()));
        return toTramo(t);
    }

    @Transactional
    public TramoQuintaResponse actualizarTramo(Integer id, TramoQuintaRequest r) {
        TramoRentaQuinta t = tramoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Tramo no existe"));
        tramoRepository.findByOrden(r.orden())
                .filter(x -> !x.getIdTramo().equals(id))
                .ifPresent(x -> { throw DomainException.conflict("Ya existe un tramo con ese orden"); });
        aplicar(t, r);
        auditar("ACTUALIZAR", "TRAMO_QUINTA", id, String.valueOf(t.getOrden()));
        return toTramo(t);
    }

    private void aplicar(TramoRentaQuinta t, TramoQuintaRequest r) {
        t.setOrden(r.orden());
        t.setHastaUit(r.hastaUit());
        t.setTasa(r.tasa());
        t.setActivo(r.activo() == null || r.activo());
    }

    private TramoQuintaResponse toTramo(TramoRentaQuinta t) {
        return new TramoQuintaResponse(t.getIdTramo(), t.getOrden(), t.getHastaUit(), t.getTasa(), t.getActivo());
    }

    // ---------------------------------------------------------------- vigencias

    @Transactional(readOnly = true)
    public List<VigenciaResponse> vigencias() {
        return vigenciaRepository.findAllByOrderByClaveAscVigenteDesdeDesc().stream().map(this::toVigencia).toList();
    }

    @Transactional
    public VigenciaResponse crearVigencia(VigenciaRequest r) {
        String clave = clave(r.clave());
        vigenciaRepository.findByClaveAndVigenteDesde(clave, r.vigenteDesde())
                .ifPresent(x -> { throw DomainException.conflict("Ya existe un valor para esa clave y fecha"); });
        ParametroVigencia v = new ParametroVigencia();
        aplicar(v, r, clave);
        vigenciaRepository.save(v);
        parametros.invalidar();
        auditar("CREAR", "VIGENCIA", v.getIdVigencia(), clave + "@" + v.getVigenteDesde());
        return toVigencia(v);
    }

    @Transactional
    public VigenciaResponse actualizarVigencia(Integer id, VigenciaRequest r) {
        ParametroVigencia v = vigenciaRepository.findById(id).orElseThrow(() -> DomainException.notFound("Vigencia no existe"));
        String clave = clave(r.clave());
        vigenciaRepository.findByClaveAndVigenteDesde(clave, r.vigenteDesde())
                .filter(x -> !x.getIdVigencia().equals(id))
                .ifPresent(x -> { throw DomainException.conflict("Ya existe un valor para esa clave y fecha"); });
        aplicar(v, r, clave);
        parametros.invalidar();
        auditar("ACTUALIZAR", "VIGENCIA", id, clave + "@" + v.getVigenteDesde());
        return toVigencia(v);
    }

    @Transactional
    public void eliminarVigencia(Integer id) {
        ParametroVigencia v = vigenciaRepository.findById(id).orElseThrow(() -> DomainException.notFound("Vigencia no existe"));
        vigenciaRepository.delete(v);
        parametros.invalidar();
        auditar("ELIMINAR", "VIGENCIA", id, v.getClave() + "@" + v.getVigenteDesde());
    }

    private void aplicar(ParametroVigencia v, VigenciaRequest r, String clave) {
        String valor = trim(r.valor());
        try {
            new java.math.BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw DomainException.badRequest("El valor de una vigencia debe ser numérico");
        }
        v.setClave(clave);
        v.setValor(valor);
        v.setVigenteDesde(r.vigenteDesde());
        v.setDescripcion(blankToNull(r.descripcion()));
    }

    private VigenciaResponse toVigencia(ParametroVigencia v) {
        return new VigenciaResponse(v.getIdVigencia(), v.getClave(), v.getValor(), v.getVigenteDesde(), v.getDescripcion());
    }

    // ---------------------------------------------------------------- utilidades

    private void auditar(String accion, String entidad, Integer id, String detalle) {
        auditoriaService.registrar(currentUser.usuario(), accion, entidad, id, detalle);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String codigo(String value) {
        return trim(value).toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String clave(String value) {
        return trim(value).toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
