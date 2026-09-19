package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.UsuarioPort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, UsuarioPort {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.empleado WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);
    Optional<Usuario> findByEmpleado_IdEmpleado(Integer idEmpleado);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreo(String correo);
    long countByRol_IdRol(Integer idRol);
}
