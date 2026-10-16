package pe.andina.rrhh.domain.model;

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
@Table(name = "adjunto_solicitud")
public class AdjuntoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto")
    private Integer idAdjunto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud_permiso", nullable = false)
    private SolicitudPermiso solicitudPermiso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paso_solicitud")
    private SolicitudPasoAprobacion paso;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_contenido", nullable = false, length = 120)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    /** Clave lógica del proveedor de almacenamiento. No es una ruta de disco. */
    @Column(name = "clave_almacen", nullable = false, length = 500)
    private String claveAlmacen;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Integer getIdAdjunto() { return idAdjunto; }
    public void setIdAdjunto(Integer idAdjunto) { this.idAdjunto = idAdjunto; }
    public SolicitudPermiso getSolicitudPermiso() { return solicitudPermiso; }
    public void setSolicitudPermiso(SolicitudPermiso solicitudPermiso) { this.solicitudPermiso = solicitudPermiso; }
    public SolicitudPasoAprobacion getPaso() { return paso; }
    public void setPaso(SolicitudPasoAprobacion paso) { this.paso = paso; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public String getClaveAlmacen() { return claveAlmacen; }
    public void setClaveAlmacen(String claveAlmacen) { this.claveAlmacen = claveAlmacen; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}