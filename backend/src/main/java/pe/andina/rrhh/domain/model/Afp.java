package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "afp")
public class Afp {

    @Id
    @Column(length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "tasa_comision", nullable = false, precision = 7, scale = 5)
    private BigDecimal tasaComision;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getTasaComision() { return tasaComision; }
    public void setTasaComision(BigDecimal tasaComision) { this.tasaComision = tasaComision; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
