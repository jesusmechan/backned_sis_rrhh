package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh_token")
    private Integer idRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String token;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(nullable = false)
    private Boolean revocado = false;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Integer getIdRefreshToken() { return idRefreshToken; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public OffsetDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(OffsetDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    public Boolean getRevocado() { return revocado; }
    public void setRevocado(Boolean revocado) { this.revocado = revocado; }
}
