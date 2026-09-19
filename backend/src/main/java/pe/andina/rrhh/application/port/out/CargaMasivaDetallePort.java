package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.CargaMasivaDetalle;

public interface CargaMasivaDetallePort {
    Optional<CargaMasivaDetalle> findById(Integer id);
    List<CargaMasivaDetalle> findAll();
    CargaMasivaDetalle save(CargaMasivaDetalle entity);
    void deleteById(Integer id);
    void delete(CargaMasivaDetalle entity);
    boolean existsById(Integer id);
    long count();

    List<CargaMasivaDetalle> findByCarga_IdCargaOrderByNumeroFilaAsc(Integer id);
}
