package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "regimen_laboral")
public class RegimenLaboral {

    @Id
    @Column(length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 250)
    private String descripcion;

    @Column(name = "factor_gratificacion", nullable = false, precision = 5, scale = 4)
    private BigDecimal factorGratificacion = BigDecimal.ONE;

    @Column(name = "factor_cts", nullable = false, precision = 5, scale = 4)
    private BigDecimal factorCts = BigDecimal.ONE;

    @Column(name = "aplica_essalud", nullable = false)
    private Boolean aplicaEssalud = true;

    @Column(name = "por_defecto", nullable = false)
    private Boolean porDefecto = false;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getFactorGratificacion() { return factorGratificacion; }
    public void setFactorGratificacion(BigDecimal factorGratificacion) { this.factorGratificacion = factorGratificacion; }
    public BigDecimal getFactorCts() { return factorCts; }
    public void setFactorCts(BigDecimal factorCts) { this.factorCts = factorCts; }
    public Boolean getAplicaEssalud() { return aplicaEssalud; }
    public void setAplicaEssalud(Boolean aplicaEssalud) { this.aplicaEssalud = aplicaEssalud; }
    public Boolean getPorDefecto() { return porDefecto; }
    public void setPorDefecto(Boolean porDefecto) { this.porDefecto = porDefecto; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
