package pe.andina.rrhh.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.RolPermiso;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.repo.RolPermisoRepository;
import pe.andina.rrhh.repo.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final RolPermisoRepository rolPermisoRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, RolPermisoRepository rolPermisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolPermisoRepository = rolPermisoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        usuario.getRol().getCodigo();
        if (usuario.getEmpleado() != null) {
            usuario.getEmpleado().getIdEmpleado();
        }
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getCodigo()));
        for (RolPermiso rp : rolPermisoRepository.findByRolId(usuario.getRol().getIdRol())) {
            authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getCodigo()));
        }
        return new UsuarioPrincipal(usuario, authorities);
    }
}
