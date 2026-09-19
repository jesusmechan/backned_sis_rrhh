package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Planilla;

public interface PlanillaPort {
    Optional<Planilla> findById(Integer id);
    List<Planilla> findAll();
    Planilla save(Planilla entity);
    void deleteById(Integer id);
    void delete(Planilla entity);
    boolean existsById(Integer id);
    long count();

    List<Planilla> findAllByOrderByAnioDescMesDesc();
    Optional<Planilla> findByAnioAndMes(Integer anio, Integer mes);
}
