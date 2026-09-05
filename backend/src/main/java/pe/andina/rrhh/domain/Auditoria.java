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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "id_entidad")
    private Integer idEntidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String detalle;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "fecha_hora", insertable = false, updatable = false)
    private OffsetDateTime fechaHora;

    public Long getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(Long idAuditoria) { this.idAuditoria = idAuditoria; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Integer getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Integer idEntidad) { this.idEntidad = idEntidad; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public String getDireccionIp() { return direccionIp; }
    public void setDireccionIp(String direccionIp) { this.direccionIp = direccionIp; }
    public OffsetDateTime getFechaHora() { return fechaHora; }
}
