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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.andina.rrhh.domain.enums.TipoOrigenFlujo;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "configuracion_aprobacion")
public class ConfiguracionAprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_origen", nullable = false)
    private TipoOrigenFlujo tipoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_permiso")
    private TipoPermiso tipoPermiso;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "configuracion")
    @OrderBy("numeroPaso ASC")
    private List<ConfiguracionAprobacionDetalle> detalles = new ArrayList<>();

    public Integer getIdConfiguracion() { return idConfiguracion; }
    public void setIdConfiguracion(Integer idConfiguracion) { this.idConfiguracion = idConfiguracion; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoOrigenFlujo getTipoOrigen() { return tipoOrigen; }
    public void setTipoOrigen(TipoOrigenFlujo tipoOrigen) { this.tipoOrigen = tipoOrigen; }
    public TipoPermiso getTipoPermiso() { return tipoPermiso; }
    public void setTipoPermiso(TipoPermiso tipoPermiso) { this.tipoPermiso = tipoPermiso; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public List<ConfiguracionAprobacionDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<ConfiguracionAprobacionDetalle> detalles) { this.detalles = detalles; }
}
