package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.RefreshTokenPort;
import pe.andina.rrhh.domain.model.RefreshToken;
import java.util.List;
import java.util.Optional;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer>, RefreshTokenPort {
    Optional<RefreshToken> findByTokenAndRevocadoFalse(String token);
    List<RefreshToken> findByUsuario_IdUsuarioAndRevocadoFalse(Integer idUsuario);
}
