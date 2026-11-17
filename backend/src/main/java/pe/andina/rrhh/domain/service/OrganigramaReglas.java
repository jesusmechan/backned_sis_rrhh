package pe.andina.rrhh.domain.service;

import org.springframework.stereotype.Component;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.NivelJerarquico;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reglas del organigrama: clasificación de nivel, ciclos y validez de jefatura.
 */
@Component
public class OrganigramaReglas {

    public NivelJerarquico nivelDe(Empleado empleado) {
        String cargo = empleado.getCargo() != null && empleado.getCargo().getNombre() != null
                ? empleado.getCargo().getNombre().trim()
                : "";
        String lower = cargo.toLowerCase(Locale.ROOT);
        if (lower.equals("gerente general")) {
            return NivelJerarquico.GERENTE_GENERAL;
        }
        if (lower.equals("subgerente general")) {
            return NivelJerarquico.SUBGERENTE_GENERAL;
        }
        if (lower.startsWith("jefe ")) {
            return NivelJerarquico.JEFE_AREA;
        }
        return NivelJerarquico.COLABORADOR;
    }

    public boolean esActivo(Empleado empleado) {
        return empleado.getEstado() == EstadoEmpleado.ACTIVO;
    }

    /**
     * Impide que el nuevo jefe sea el propio empleado o un subordinado suyo (ciclo).
     */
    public void validarNuevaJefatura(Empleado empleado, Empleado nuevoJefe, List<Empleado> todos) {
        if (nuevoJefe == null) {
            return;
        }
        if (Objects.equals(empleado.getIdEmpleado(), nuevoJefe.getIdEmpleado())) {
            throw DomainException.badRequest("El trabajador no puede ser su propio jefe");
        }
        if (!esActivo(nuevoJefe)) {
            throw DomainException.badRequest("El jefe inmediato debe estar activo");
        }
        Map<Integer, Empleado> porId = todos.stream()
                .filter(e -> e.getIdEmpleado() != null)
                .collect(Collectors.toMap(Empleado::getIdEmpleado, Function.identity(), (a, b) -> a));

        Set<Integer> visitados = new HashSet<>();
        Empleado cursor = nuevoJefe;
        while (cursor != null) {
            Integer id = cursor.getIdEmpleado();
            if (id == null || !visitados.add(id)) {
                throw DomainException.badRequest("La cadena de jefatura forma un ciclo");
            }
            if (Objects.equals(id, empleado.getIdEmpleado())) {
                throw DomainException.badRequest("No puede reportar a un subordinado (generaría un ciclo)");
            }
            Empleado jefe = cursor.getJefeInmediato();
            if (jefe == null || jefe.getIdEmpleado() == null) {
                break;
            }
            cursor = porId.getOrDefault(jefe.getIdEmpleado(), jefe);
        }
    }

    public void exigirActivo(Empleado empleado, String mensaje) {
        if (!esActivo(empleado)) {
            throw DomainException.badRequest(mensaje);
        }
    }
}
