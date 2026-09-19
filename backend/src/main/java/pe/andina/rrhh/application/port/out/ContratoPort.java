package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;

public interface ContratoPort {
    Optional<Contrato> findById(Integer id);
    List<Contrato> findAll();
    Contrato save(Contrato entity);
    void deleteById(Integer id);
    void delete(Contrato entity);
    boolean existsById(Integer id);
    long count();

    List<Contrato> findAllByOrderByIdContratoDesc();
    List<Contrato> findByEmpleado_IdEmpleadoOrderByFechaInicioDesc(Integer idEmpleado);
    Optional<Contrato> findFirstByEmpleado_IdEmpleadoAndEstado(Integer idEmpleado, EstadoContrato estado);
    Optional<Contrato> findByCodigo(String codigo);
}
