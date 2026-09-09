package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Contrato;
import pe.andina.rrhh.domain.enums.EstadoContrato;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Integer> {
    List<Contrato> findAllByOrderByIdContratoDesc();
    List<Contrato> findByEmpleado_IdEmpleadoOrderByFechaInicioDesc(Integer idEmpleado);
    Optional<Contrato> findFirstByEmpleado_IdEmpleadoAndEstado(Integer idEmpleado, EstadoContrato estado);
    Optional<Contrato> findByCodigo(String codigo);
}
