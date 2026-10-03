package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tramo_renta_quinta")
public class TramoRentaQuinta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tramo")
    private Integer idTramo;

    @Column(nullable = false)
    private Integer orden;

    /** Límite superior acumulado en UIT; {@code null} en el último tramo. */
    @Column(name = "hasta_uit", precision = 8, scale = 2)
    private BigDecimal hastaUit;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal tasa;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdTramo() { return idTramo; }
    public void setIdTramo(Integer idTramo) { this.idTramo = idTramo; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public BigDecimal getHastaUit() { return hastaUit; }
    public void setHastaUit(BigDecimal hastaUit) { this.hastaUit = hastaUit; }
    public BigDecimal getTasa() { return tasa; }
    public void setTasa(BigDecimal tasa) { this.tasa = tasa; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
