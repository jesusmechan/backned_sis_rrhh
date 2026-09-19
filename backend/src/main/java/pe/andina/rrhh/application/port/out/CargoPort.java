package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Cargo;

public interface CargoPort {
    Optional<Cargo> findById(Integer id);
    List<Cargo> findAll();
    Cargo save(Cargo entity);
    void deleteById(Integer id);
    void delete(Cargo entity);
    boolean existsById(Integer id);
    long count();

    Optional<Cargo> findByNombreIgnoreCase(String nombre);
}
