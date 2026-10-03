package pe.andina.rrhh.application.port.out;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

/**
 * Lectura de parámetros configurables. Una clave inexistente es un error de configuración:
 * no hay valores por defecto en el código.
 */
public interface ParametroPort {
    BigDecimal decimal(String clave);

    /** Valor vigente a la fecha (parametro_vigencia); si no hay, el de parametro_sistema. */
    BigDecimal decimal(String clave, LocalDate fecha);

    int entero(String clave);

    String texto(String clave);

    ZoneId zona();

    Set<DayOfWeek> diasLaborables();

    void invalidar();
}
