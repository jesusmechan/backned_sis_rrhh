package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Postulacion;

public interface PostulacionPort {
    Optional<Postulacion> findById(Integer id);
    List<Postulacion> findAll();
    Postulacion save(Postulacion entity);
    void deleteById(Integer id);
    void delete(Postulacion entity);
    boolean existsById(Integer id);
    long count();

    List<Postulacion> findByConvocatoria_IdConvocatoriaOrderByIdPostulacionDesc(Integer idConvocatoria);
    long countByConvocatoria_IdConvocatoria(Integer idConvocatoria);
}
