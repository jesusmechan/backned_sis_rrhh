package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.CargaMasivaDetalle;
import java.util.List;
public interface CargaMasivaDetalleRepository extends JpaRepository<CargaMasivaDetalle, Integer> {
    List<CargaMasivaDetalle> findByCarga_IdCargaOrderByNumeroFilaAsc(Integer id);
}
