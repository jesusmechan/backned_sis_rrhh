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

import java.math.BigDecimal;

@Entity
@Table(name = "asiento_linea")
public class AsientoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_linea")
    private Integer idLinea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asiento", nullable = false)
    private AsientoContable asiento;

    @Column(nullable = false, length = 20)
    private String cuenta;

    @Column(name = "nombre_cuenta", nullable = false, length = 80)
    private String nombreCuenta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;

    public Integer getIdLinea() { return idLinea; }
    public void setIdLinea(Integer idLinea) { this.idLinea = idLinea; }
    public AsientoContable getAsiento() { return asiento; }
    public void setAsiento(AsientoContable asiento) { this.asiento = asiento; }
    public String getCuenta() { return cuenta; }
    public void setCuenta(String cuenta) { this.cuenta = cuenta; }
    public String getNombreCuenta() { return nombreCuenta; }
    public void setNombreCuenta(String nombreCuenta) { this.nombreCuenta = nombreCuenta; }
    public BigDecimal getDebe() { return debe; }
    public void setDebe(BigDecimal debe) { this.debe = debe; }
    public BigDecimal getHaber() { return haber; }
    public void setHaber(BigDecimal haber) { this.haber = haber; }
}
