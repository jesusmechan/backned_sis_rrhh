package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.CatalogoValor;

public interface CatalogoValorPort {
    Optional<CatalogoValor> findById(Integer id);
    Optional<CatalogoValor> findByTipoAndCodigo(String tipo, String codigo);
    Optional<CatalogoValor> findFirstByTipoAndPorDefectoTrueAndActivoTrue(String tipo);
    List<CatalogoValor> findByTipo(String tipo);
    List<CatalogoValor> findAllByOrderByTipoAscOrdenAscNombreAsc();
    CatalogoValor save(CatalogoValor entity);
}
