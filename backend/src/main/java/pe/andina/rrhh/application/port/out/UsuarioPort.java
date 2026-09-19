package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Usuario;

public interface UsuarioPort {
    Optional<Usuario> findById(Integer id);
    List<Usuario> findAll();
    Usuario save(Usuario entity);
    void deleteById(Integer id);
    void delete(Usuario entity);
    boolean existsById(Integer id);
    long count();

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByEmpleado_IdEmpleado(Integer idEmpleado);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByCorreo(String correo);
    long countByRol_IdRol(Integer idRol);
}
