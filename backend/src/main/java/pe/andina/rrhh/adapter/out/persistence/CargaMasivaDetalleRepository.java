package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CargaMasivaDetallePort;
import pe.andina.rrhh.domain.model.CargaMasivaDetalle;
import java.util.List;
public interface CargaMasivaDetalleRepository extends JpaRepository<CargaMasivaDetalle, Integer>, CargaMasivaDetallePort {
    List<CargaMasivaDetalle> findByCarga_IdCargaOrderByNumeroFilaAsc(Integer id);
}
