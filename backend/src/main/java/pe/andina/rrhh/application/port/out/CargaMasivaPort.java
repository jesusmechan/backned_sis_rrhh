package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.CargaMasiva;

public interface CargaMasivaPort {
    Optional<CargaMasiva> findById(Integer id);
    List<CargaMasiva> findAll();
    CargaMasiva save(CargaMasiva entity);
    void deleteById(Integer id);
    void delete(CargaMasiva entity);
    boolean existsById(Integer id);
    long count();
}
