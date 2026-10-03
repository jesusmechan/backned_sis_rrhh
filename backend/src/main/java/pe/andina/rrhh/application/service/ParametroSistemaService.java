package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.ParametroSistemaPort;
import pe.andina.rrhh.application.port.out.ParametroVigenciaPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.ParametroSistema;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParametroSistemaService implements ParametroPort {

    static final String ZONA_HORARIA = "zona_horaria";
    static final String DIAS_LABORABLES = "dias_laborables";

    private final ParametroSistemaPort parametroRepository;
    private final ParametroVigenciaPort vigenciaRepository;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public ParametroSistemaService(ParametroSistemaPort parametroRepository,
                                   ParametroVigenciaPort vigenciaRepository) {
        this.parametroRepository = parametroRepository;
        this.vigenciaRepository = vigenciaRepository;
    }

    @Override
    public BigDecimal decimal(String clave) {
        return aDecimal(clave, texto(clave));
    }

    @Override
    public BigDecimal decimal(String clave, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(zona());
        return vigenciaRepository
                .findFirstByClaveAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(clave, dia)
                .map(v -> aDecimal(clave, v.getValor()))
                .orElseGet(() -> decimal(clave));
    }

    @Override
    public int entero(String clave) {
        try {
            return decimal(clave).intValueExact();
        } catch (ArithmeticException e) {
            throw invalido(clave, "debe ser un número entero");
        }
    }

    @Override
    public String texto(String clave) {
        return cache.computeIfAbsent(clave, k -> parametroRepository.findById(k)
                        .map(ParametroSistema::getValor)
                        .map(String::trim)
                        .filter(v -> !v.isEmpty()))
                .orElseThrow(() -> DomainException.badRequest(
                        "Falta configurar el parámetro «" + clave + "» en Maestros › Parámetros"));
    }

    @Override
    public ZoneId zona() {
        try {
            return ZoneId.of(texto(ZONA_HORARIA));
        } catch (DateTimeException e) {
            throw invalido(ZONA_HORARIA, "no es una zona horaria válida");
        }
    }

    @Override
    public Set<DayOfWeek> diasLaborables() {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (String parte : texto(DIAS_LABORABLES).split(",")) {
            try {
                dias.add(DayOfWeek.of(Integer.parseInt(parte.trim())));
            } catch (RuntimeException e) {
                throw invalido(DIAS_LABORABLES, "debe listar días del 1 (lunes) al 7 (domingo) separados por coma");
            }
        }
        return dias;
    }

    @Override
    public void invalidar() {
        cache.clear();
    }

    private BigDecimal aDecimal(String clave, String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            throw invalido(clave, "debe ser numérico");
        }
    }

    private static DomainException invalido(String clave, String motivo) {
        return DomainException.badRequest("El parámetro «" + clave + "» " + motivo);
    }
}
