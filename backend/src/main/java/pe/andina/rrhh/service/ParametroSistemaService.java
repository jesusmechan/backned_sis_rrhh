package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.repo.ParametroSistemaRepository;

import java.math.BigDecimal;

@Service
public class ParametroSistemaService {

    private final ParametroSistemaRepository parametroRepository;

    public ParametroSistemaService(ParametroSistemaRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    public BigDecimal decimal(String clave, BigDecimal defecto) {
        return parametroRepository.findById(clave)
                .map(p -> {
                    try {
                        return new BigDecimal(p.getValor().trim());
                    } catch (NumberFormatException e) {
                        return defecto;
                    }
                })
                .orElse(defecto);
    }

    public BigDecimal decimal(String clave, String defecto) {
        return decimal(clave, new BigDecimal(defecto));
    }

    public String texto(String clave, String defecto) {
        return parametroRepository.findById(clave)
                .map(p -> p.getValor() == null || p.getValor().isBlank() ? defecto : p.getValor().trim())
                .orElse(defecto);
    }
}
