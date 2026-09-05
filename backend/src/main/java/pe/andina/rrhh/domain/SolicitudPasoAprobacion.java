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
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.enums.TipoAprobador;

import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitud_paso_aprobacion")
public class SolicitudPasoAprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paso_solicitud")
    private Integer idPasoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_permiso")
    private SolicitudPermiso solicitudPermiso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_hora_extra")
    private SolicitudHoraExtra solicitudHoraExtra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_configuracion", nullable = false)
    private ConfiguracionAprobacion configuracion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle", nullable = false)
    private ConfiguracionAprobacionDetalle detalle;

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
    @JoinColumn(name = "id_usuario_asignado")
    private Usuario usuarioAsignado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoPasoAprobacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_decision")
    private Usuario usuarioDecision;

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_decision")
    private OffsetDateTime fechaDecision;

    @Column(length = 400)
    private String comentario;

    public Integer getIdPasoSolicitud() { return idPasoSolicitud; }
    public void setIdPasoSolicitud(Integer idPasoSolicitud) { this.idPasoSolicitud = idPasoSolicitud; }
    public SolicitudPermiso getSolicitudPermiso() { return solicitudPermiso; }
    public void setSolicitudPermiso(SolicitudPermiso solicitudPermiso) { this.solicitudPermiso = solicitudPermiso; }
    public SolicitudHoraExtra getSolicitudHoraExtra() { return solicitudHoraExtra; }
    public void setSolicitudHoraExtra(SolicitudHoraExtra solicitudHoraExtra) { this.solicitudHoraExtra = solicitudHoraExtra; }
    public ConfiguracionAprobacion getConfiguracion() { return configuracion; }
    public void setConfiguracion(ConfiguracionAprobacion configuracion) { this.configuracion = configuracion; }
    public ConfiguracionAprobacionDetalle getDetalle() { return detalle; }
    public void setDetalle(ConfiguracionAprobacionDetalle detalle) { this.detalle = detalle; }
    public Integer getNumeroPaso() { return numeroPaso; }
    public void setNumeroPaso(Integer numeroPaso) { this.numeroPaso = numeroPaso; }
    public String getNombrePaso() { return nombrePaso; }
    public void setNombrePaso(String nombrePaso) { this.nombrePaso = nombrePaso; }
    public TipoAprobador getTipoAprobador() { return tipoAprobador; }
    public void setTipoAprobador(TipoAprobador tipoAprobador) { this.tipoAprobador = tipoAprobador; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Usuario getUsuarioAsignado() { return usuarioAsignado; }
    public void setUsuarioAsignado(Usuario usuarioAsignado) { this.usuarioAsignado = usuarioAsignado; }
    public EstadoPasoAprobacion getEstado() { return estado; }
    public void setEstado(EstadoPasoAprobacion estado) { this.estado = estado; }
    public Usuario getUsuarioDecision() { return usuarioDecision; }
    public void setUsuarioDecision(Usuario usuarioDecision) { this.usuarioDecision = usuarioDecision; }
    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(OffsetDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public OffsetDateTime getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(OffsetDateTime fechaDecision) { this.fechaDecision = fechaDecision; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
