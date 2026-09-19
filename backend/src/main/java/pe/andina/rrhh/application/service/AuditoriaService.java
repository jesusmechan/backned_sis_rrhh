package pe.andina.rrhh.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.model.Auditoria;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.application.port.out.AuditoriaPort;

@Service
public class AuditoriaService implements AuditoriaUseCase {

    private final AuditoriaPort auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaPort auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(Usuario usuario, String accion, String entidad, Integer idEntidad, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setAccion(accion);
        a.setEntidad(entidad);
        a.setIdEntidad(idEntidad);
        a.setDetalle(asJson(detalle));
        auditoriaRepository.save(a);
    }

    private String asJson(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return null;
        }
        String trimmed = detalle.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ignored) {
            try {
                return objectMapper.writeValueAsString(detalle);
            } catch (JsonProcessingException e) {
                return "null";
            }
        }
    }
}
