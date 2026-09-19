package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.RefreshToken;

public interface RefreshTokenPort {
    Optional<RefreshToken> findById(Integer id);
    List<RefreshToken> findAll();
    RefreshToken save(RefreshToken entity);
    void deleteById(Integer id);
    void delete(RefreshToken entity);
    boolean existsById(Integer id);
    long count();

    Optional<RefreshToken> findByTokenAndRevocadoFalse(String token);
    List<RefreshToken> findByUsuario_IdUsuarioAndRevocadoFalse(Integer idUsuario);
}
