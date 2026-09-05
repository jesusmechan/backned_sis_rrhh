package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.Auditoria;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.repo.AuditoriaRepository;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public void registrar(Usuario usuario, String accion, String entidad, Integer idEntidad, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setAccion(accion);
        a.setEntidad(entidad);
        a.setIdEntidad(idEntidad);
        a.setDetalle(detalle);
        auditoriaRepository.save(a);
    }
}
