package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.andina.rrhh.domain.model.enums.EstadoPlanilla;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "planilla")
public class Planilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planilla")
    private Integer idPlanilla;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoPlanilla estado = EstadoPlanilla.BORRADOR;

    @Column(name = "total_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBruto = BigDecimal.ZERO;

    @Column(name = "total_descuentos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_aportes", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAportes = BigDecimal.ZERO;

    @Column(name = "total_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNeto = BigDecimal.ZERO;

    @Column(length = 400)
    private String observaciones;

    @Column(name = "fecha_calculo")
    private OffsetDateTime fechaCalculo;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Integer getIdPlanilla() { return idPlanilla; }
    public void setIdPlanilla(Integer idPlanilla) { this.idPlanilla = idPlanilla; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public EstadoPlanilla getEstado() { return estado; }
    public void setEstado(EstadoPlanilla estado) { this.estado = estado; }
    public BigDecimal getTotalBruto() { return totalBruto; }
    public void setTotalBruto(BigDecimal totalBruto) { this.totalBruto = totalBruto; }
    public BigDecimal getTotalDescuentos() { return totalDescuentos; }
    public void setTotalDescuentos(BigDecimal totalDescuentos) { this.totalDescuentos = totalDescuentos; }
    public BigDecimal getTotalAportes() { return totalAportes; }
    public void setTotalAportes(BigDecimal totalAportes) { this.totalAportes = totalAportes; }
    public BigDecimal getTotalNeto() { return totalNeto; }
    public void setTotalNeto(BigDecimal totalNeto) { this.totalNeto = totalNeto; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public OffsetDateTime getFechaCalculo() { return fechaCalculo; }
    public void setFechaCalculo(OffsetDateTime fechaCalculo) { this.fechaCalculo = fechaCalculo; }
    public OffsetDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(OffsetDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
