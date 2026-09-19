package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.PostulacionPort;
import pe.andina.rrhh.domain.model.Postulacion;

import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Integer>, PostulacionPort {
    List<Postulacion> findByConvocatoria_IdConvocatoriaOrderByIdPostulacionDesc(Integer idConvocatoria);
    long countByConvocatoria_IdConvocatoria(Integer idConvocatoria);
}
