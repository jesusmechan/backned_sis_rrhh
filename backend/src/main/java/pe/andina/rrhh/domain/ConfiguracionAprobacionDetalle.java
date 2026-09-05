package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.andina.rrhh.domain.enums.TipoAprobador;

@Entity
@Table(name = "configuracion_aprobacion_detalle")
public class ConfiguracionAprobacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_configuracion", nullable = false)
    private ConfiguracionAprobacion configuracion;

    @Column(name = "numero_paso", nullable = false)
    private Integer numeroPaso;

    @Column(name = "nombre_paso", nullable = false, length = 120)
    private String nombrePaso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_aprobador", nullable = false)
    private TipoAprobador tipoAprobador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "es_obligatorio", nullable = false)
    private Boolean esObligatorio = true;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
    public ConfiguracionAprobacion getConfiguracion() { return configuracion; }
    public void setConfiguracion(ConfiguracionAprobacion configuracion) { this.configuracion = configuracion; }
    public Integer getNumeroPaso() { return numeroPaso; }
    public void setNumeroPaso(Integer numeroPaso) { this.numeroPaso = numeroPaso; }
    public String getNombrePaso() { return nombrePaso; }
    public void setNombrePaso(String nombrePaso) { this.nombrePaso = nombrePaso; }
    public TipoAprobador getTipoAprobador() { return tipoAprobador; }
    public void setTipoAprobador(TipoAprobador tipoAprobador) { this.tipoAprobador = tipoAprobador; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Boolean getEsObligatorio() { return esObligatorio; }
    public void setEsObligatorio(Boolean esObligatorio) { this.esObligatorio = esObligatorio; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
