package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.ConsultaUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.application.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.port.out.AuditoriaPort;
import pe.andina.rrhh.application.port.out.HistorialSolicitudPort;

import java.util.List;

@Service
public class ConsultaService implements ConsultaUseCase {

    private final AuditoriaPort auditoriaRepository;
    private final HistorialSolicitudPort historialRepository;

    public ConsultaService(AuditoriaPort auditoriaRepository,
                           HistorialSolicitudPort historialRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.historialRepository = historialRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> auditoria() {
        return auditoriaRepository.findTop200ByOrderByIdAuditoriaDesc().stream().map(DtoMapper::auditoria).toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> trazabilidad() {
        return historialRepository.findAll().stream().map(DtoMapper::historial).toList();
    }
}
