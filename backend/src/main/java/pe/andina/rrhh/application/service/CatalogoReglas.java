package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.out.CatalogoValorPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.CatalogoValor;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Valores por defecto y reglas de formato definidos en Maestros › Catálogos. */
@Service
public class CatalogoReglas {

    public static final String TIPO_DOCUMENTO = "TIPO_DOCUMENTO";
    public static final String SEXO = "SEXO";
    public static final String TIPO_CONTRATO = "TIPO_CONTRATO";
    public static final String ESTADO_EMPLEADO = "ESTADO_EMPLEADO";
    public static final String MODALIDAD_CONTRATO = "MODALIDAD_CONTRATO";
    public static final String REGIMEN_PENSIONARIO = "REGIMEN_PENSIONARIO";
    public static final String ESTADO_CONTRATO = "ESTADO_CONTRATO";

    private final CatalogoValorPort valorRepository;

    public CatalogoReglas(CatalogoValorPort valorRepository) {
        this.valorRepository = valorRepository;
    }

    public <E extends Enum<E>> E porDefecto(String tipo, Class<E> clase) {
        CatalogoValor valor = valorRepository.findFirstByTipoAndPorDefectoTrueAndActivoTrue(tipo)
                .orElseThrow(() -> DomainException.badRequest(
                        "Marque un valor por defecto para el catálogo «" + tipo + "» en Maestros › Catálogos"));
        try {
            return Enum.valueOf(clase, valor.getCodigo());
        } catch (IllegalArgumentException e) {
            throw DomainException.badRequest("El valor por defecto «" + valor.getCodigo() + "» del catálogo «" + tipo + "» no es válido");
        }
    }

    public <E extends Enum<E>> E oPorDefecto(E valor, String tipo, Class<E> clase) {
        return valor != null ? valor : porDefecto(tipo, clase);
    }

    /** Aplica la expresión regular configurada para el código del catálogo, si la hay. */
    public void validarRegla(String tipo, String codigo, String valor, String campo) {
        if (codigo == null || valor == null) {
            return;
        }
        CatalogoValor item = valorRepository.findByTipoAndCodigo(tipo, codigo).orElse(null);
        if (item == null || item.getRegla() == null || item.getRegla().isBlank()) {
            return;
        }
        boolean ok;
        try {
            ok = Pattern.matches(item.getRegla(), valor);
        } catch (PatternSyntaxException e) {
            throw DomainException.badRequest("La regla del catálogo «" + tipo + " / " + codigo + "» no es una expresión válida");
        }
        if (!ok) {
            String mensaje = item.getMensajeRegla() != null && !item.getMensajeRegla().isBlank()
                    ? item.getMensajeRegla()
                    : campo + " no tiene el formato esperado para " + item.getNombre();
            throw DomainException.badRequest(mensaje);
        }
    }
}
