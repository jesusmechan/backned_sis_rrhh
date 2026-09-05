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
import pe.andina.rrhh.domain.enums.AccionSolicitud;
import pe.andina.rrhh.domain.enums.EstadoSolicitud;

import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_solicitud")
public class HistorialSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_permiso")
    private SolicitudPermiso solicitudPermiso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_hora_extra")
    private SolicitudHoraExtra solicitudHoraExtra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paso_solicitud")
    private SolicitudPasoAprobacion paso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AccionSolicitud accion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_anterior")
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoSolicitud estadoNuevo;

    @Column(length = 400)
    private String comentario;

    @Column(name = "fecha_hora", insertable = false, updatable = false)
    private OffsetDateTime fechaHora;

    public Integer getIdHistorial() { return idHistorial; }
    public SolicitudPermiso getSolicitudPermiso() { return solicitudPermiso; }
    public SolicitudHoraExtra getSolicitudHoraExtra() { return solicitudHoraExtra; }
    public SolicitudPasoAprobacion getPaso() { return paso; }
    public Usuario getUsuario() { return usuario; }
    public AccionSolicitud getAccion() { return accion; }
    public EstadoSolicitud getEstadoAnterior() { return estadoAnterior; }
    public EstadoSolicitud getEstadoNuevo() { return estadoNuevo; }
    public String getComentario() { return comentario; }
    public OffsetDateTime getFechaHora() { return fechaHora; }
}
