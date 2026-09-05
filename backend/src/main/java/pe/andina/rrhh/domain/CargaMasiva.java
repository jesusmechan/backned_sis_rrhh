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
import pe.andina.rrhh.domain.enums.EstadoCarga;

import java.time.OffsetDateTime;

@Entity
@Table(name = "carga_masiva")
public class CargaMasiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carga")
    private Integer idCarga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "total_filas", nullable = false)
    private Integer totalFilas = 0;

    @Column(name = "filas_exitosas", nullable = false)
    private Integer filasExitosas = 0;

    @Column(name = "filas_fallidas", nullable = false)
    private Integer filasFallidas = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoCarga estado = EstadoCarga.PROCESANDO;

    @Column(length = 400)
    private String mensaje;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    public Integer getIdCarga() { return idCarga; }
    public void setIdCarga(Integer idCarga) { this.idCarga = idCarga; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public Integer getTotalFilas() { return totalFilas; }
    public void setTotalFilas(Integer totalFilas) { this.totalFilas = totalFilas; }
    public Integer getFilasExitosas() { return filasExitosas; }
    public void setFilasExitosas(Integer filasExitosas) { this.filasExitosas = filasExitosas; }
    public Integer getFilasFallidas() { return filasFallidas; }
    public void setFilasFallidas(Integer filasFallidas) { this.filasFallidas = filasFallidas; }
    public EstadoCarga getEstado() { return estado; }
    public void setEstado(EstadoCarga estado) { this.estado = estado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public OffsetDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(OffsetDateTime fechaFin) { this.fechaFin = fechaFin; }
}
