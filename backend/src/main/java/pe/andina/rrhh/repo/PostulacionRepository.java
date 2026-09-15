package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Postulacion;

import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {
    List<Postulacion> findByConvocatoria_IdConvocatoriaOrderByIdPostulacionDesc(Integer idConvocatoria);
    long countByConvocatoria_IdConvocatoria(Integer idConvocatoria);
}
