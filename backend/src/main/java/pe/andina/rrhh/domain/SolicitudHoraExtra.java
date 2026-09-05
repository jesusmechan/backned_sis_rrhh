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
import pe.andina.rrhh.domain.enums.EstadoSolicitud;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitud_hora_extra")
public class SolicitudHoraExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud_hora_extra")
    private Integer idSolicitudHoraExtra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_configuracion", insertable = false, updatable = false)
    private ConfiguracionAprobacion configuracion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "cantidad_horas", nullable = false, precision = 5, scale = 2)
    private BigDecimal cantidadHoras;

    @Column(nullable = false, length = 400)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Integer getIdSolicitudHoraExtra() { return idSolicitudHoraExtra; }
    public void setIdSolicitudHoraExtra(Integer idSolicitudHoraExtra) { this.idSolicitudHoraExtra = idSolicitudHoraExtra; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public ConfiguracionAprobacion getConfiguracion() { return configuracion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public BigDecimal getCantidadHoras() { return cantidadHoras; }
    public void setCantidadHoras(BigDecimal cantidadHoras) { this.cantidadHoras = cantidadHoras; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
