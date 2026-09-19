package pe.andina.rrhh.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Marcacion;
import pe.andina.rrhh.domain.model.enums.TipoMarcacion;

public interface MarcacionPort {
    Optional<Marcacion> findById(Integer id);
    List<Marcacion> findAll();
    Marcacion save(Marcacion entity);
    Marcacion saveAndFlush(Marcacion entity);
    void deleteById(Integer id);
    void delete(Marcacion entity);
    boolean existsById(Integer id);
    long count();

    List<Marcacion> findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(Integer idEmpleado);
    List<Marcacion> findByFechaBetweenOrderByFechaHoraDesc(LocalDate desde, LocalDate hasta);
    List<Marcacion> findByEmpleado_IdEmpleadoAndTipoAndFecha(Integer idEmpleado, TipoMarcacion tipo, LocalDate fecha);
}
