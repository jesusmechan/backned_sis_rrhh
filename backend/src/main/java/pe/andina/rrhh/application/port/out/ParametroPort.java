package pe.andina.rrhh.application.port.out;

import java.math.BigDecimal;

public interface ParametroPort {
    BigDecimal decimal(String clave, BigDecimal defecto);
    BigDecimal decimal(String clave, String defecto);
    String texto(String clave, String defecto);
}
