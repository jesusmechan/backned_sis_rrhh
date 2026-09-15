package pe.andina.rrhh.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.andina.rrhh.domain.enums.EstadoAsiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asiento_contable")
public class AsientoContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asiento")
    private Integer idAsiento;

    @Column(nullable = false, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_planilla")
    private Planilla planilla;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 200)
    private String glosa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoAsiento estado = EstadoAsiento.CONTABILIZADO;

    @Column(name = "total_debe", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDebe = BigDecimal.ZERO;

    @Column(name = "total_haber", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalHaber = BigDecimal.ZERO;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AsientoLinea> lineas = new ArrayList<>();

    public Integer getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Integer idAsiento) { this.idAsiento = idAsiento; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Planilla getPlanilla() { return planilla; }
    public void setPlanilla(Planilla planilla) { this.planilla = planilla; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getGlosa() { return glosa; }
    public void setGlosa(String glosa) { this.glosa = glosa; }
    public EstadoAsiento getEstado() { return estado; }
    public void setEstado(EstadoAsiento estado) { this.estado = estado; }
    public BigDecimal getTotalDebe() { return totalDebe; }
    public void setTotalDebe(BigDecimal totalDebe) { this.totalDebe = totalDebe; }
    public BigDecimal getTotalHaber() { return totalHaber; }
    public void setTotalHaber(BigDecimal totalHaber) { this.totalHaber = totalHaber; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public List<AsientoLinea> getLineas() { return lineas; }
    public void setLineas(List<AsientoLinea> lineas) { this.lineas = lineas; }

    public void addLinea(AsientoLinea linea) {
        linea.setAsiento(this);
        lineas.add(linea);
    }
}
