package pe.andina.rrhh.application.port.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.andina.rrhh.domain.model.Auditoria;
import pe.andina.rrhh.domain.model.Usuario;

public interface AuditoriaUseCase {
    void registrar(Usuario usuario, String accion, String entidad, Integer idEntidad, String detalle);
}
