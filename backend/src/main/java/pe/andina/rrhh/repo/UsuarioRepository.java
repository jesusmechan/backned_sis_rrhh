package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.empleado WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);
    Optional<Usuario> findByEmpleado_IdEmpleado(Integer idEmpleado);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreo(String correo);
}
