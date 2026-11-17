package pe.andina.rrhh.domain.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitud_adjunto")
public class SolicitudAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto")
    private Integer idAdjunto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_permiso")
    private SolicitudPermiso solicitudPermiso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud_hora_extra")
    private SolicitudHoraExtra solicitudHoraExtra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paso_solicitud")
    private SolicitudPasoAprobacion paso;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "tamanio_bytes", nullable = false)
    private Integer tamanioBytes;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "contenido", nullable = false)
    private byte[] contenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_carga", nullable = false)
    private Usuario usuarioCarga;

    @Column(name = "fecha_carga", insertable = false, updatable = false)
    private OffsetDateTime fechaCarga;

    public Integer getIdAdjunto() { return idAdjunto; }
    public void setIdAdjunto(Integer idAdjunto) { this.idAdjunto = idAdjunto; }
    public SolicitudPermiso getSolicitudPermiso() { return solicitudPermiso; }
    public void setSolicitudPermiso(SolicitudPermiso solicitudPermiso) { this.solicitudPermiso = solicitudPermiso; }
    public SolicitudHoraExtra getSolicitudHoraExtra() { return solicitudHoraExtra; }
    public void setSolicitudHoraExtra(SolicitudHoraExtra solicitudHoraExtra) { this.solicitudHoraExtra = solicitudHoraExtra; }
    public SolicitudPasoAprobacion getPaso() { return paso; }
    public void setPaso(SolicitudPasoAprobacion paso) { this.paso = paso; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Integer getTamanioBytes() { return tamanioBytes; }
    public void setTamanioBytes(Integer tamanioBytes) { this.tamanioBytes = tamanioBytes; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public Usuario getUsuarioCarga() { return usuarioCarga; }
    public void setUsuarioCarga(Usuario usuarioCarga) { this.usuarioCarga = usuarioCarga; }
    public OffsetDateTime getFechaCarga() { return fechaCarga; }
}
