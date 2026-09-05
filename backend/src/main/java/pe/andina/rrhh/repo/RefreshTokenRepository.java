package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByTokenAndRevocadoFalse(String token);
    List<RefreshToken> findByUsuario_IdUsuarioAndRevocadoFalse(Integer idUsuario);
}
