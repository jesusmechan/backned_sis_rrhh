package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.repo.AuditoriaRepository;
import pe.andina.rrhh.repo.HistorialSolicitudRepository;

import java.util.List;

@Service
public class ConsultaService {

    private final AuditoriaRepository auditoriaRepository;
    private final HistorialSolicitudRepository historialRepository;

    public ConsultaService(AuditoriaRepository auditoriaRepository,
                           HistorialSolicitudRepository historialRepository) {
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
