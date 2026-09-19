package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ContratoPort;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Integer>, ContratoPort {
    List<Contrato> findAllByOrderByIdContratoDesc();
    List<Contrato> findByEmpleado_IdEmpleadoOrderByFechaInicioDesc(Integer idEmpleado);
    Optional<Contrato> findFirstByEmpleado_IdEmpleadoAndEstado(Integer idEmpleado, EstadoContrato estado);
    Optional<Contrato> findByCodigo(String codigo);
}
