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
import pe.andina.rrhh.domain.enums.TipoMarcacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "marcacion")
public class Marcacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marcacion")
    private Integer idMarcacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TipoMarcacion tipo;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    @Column(insertable = false, updatable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 20)
    private String origen = "WEB";

    @Column(length = 250)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro")
    private Usuario usuarioRegistro;

    public Integer getIdMarcacion() { return idMarcacion; }
    public void setIdMarcacion(Integer idMarcacion) { this.idMarcacion = idMarcacion; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public TipoMarcacion getTipo() { return tipo; }
    public void setTipo(TipoMarcacion tipo) { this.tipo = tipo; }
    public OffsetDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(OffsetDateTime fechaHora) { this.fechaHora = fechaHora; }
    public LocalDate getFecha() { return fecha; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Usuario getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(Usuario usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
