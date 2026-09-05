package pe.andina.rrhh.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.andina.rrhh.domain.Usuario;

import java.util.Collection;

public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(Usuario usuario, Collection<? extends GrantedAuthority> authorities) {
        this.usuario = usuario;
        this.authorities = authorities;
    }

    public Usuario getUsuario() { return usuario; }
    public Integer getIdUsuario() { return usuario.getIdUsuario(); }
    public String getCodigoRol() { return usuario.getRol().getCodigo(); }
    public Integer getIdEmpleado() {
        return usuario.getEmpleado() != null ? usuario.getEmpleado().getIdEmpleado() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override
    public String getPassword() { return usuario.getPasswordHash(); }
    @Override
    public String getUsername() { return usuario.getNombreUsuario(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return Boolean.TRUE.equals(usuario.getActivo()); }
}
