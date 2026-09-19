package pe.andina.rrhh.domain.model;

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
import pe.andina.rrhh.domain.model.enums.EstadoEvaluacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evaluacion_desempeno")
public class EvaluacionDesempeno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Integer idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluador")
    private Usuario evaluador;

    @Column(nullable = false, length = 20)
    private String periodo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer puntualidad;

    @Column(nullable = false)
    private Integer calidad;

    @Column(nullable = false)
    private Integer cooperacion;

    @Column(nullable = false)
    private Integer iniciativa;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal promedio;

    @Column(length = 400)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoEvaluacion estado = EstadoEvaluacion.CERRADA;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    public Integer getIdEvaluacion() { return idEvaluacion; }
    public void setIdEvaluacion(Integer idEvaluacion) { this.idEvaluacion = idEvaluacion; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public Usuario getEvaluador() { return evaluador; }
    public void setEvaluador(Usuario evaluador) { this.evaluador = evaluador; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Integer getPuntualidad() { return puntualidad; }
    public void setPuntualidad(Integer puntualidad) { this.puntualidad = puntualidad; }
    public Integer getCalidad() { return calidad; }
    public void setCalidad(Integer calidad) { this.calidad = calidad; }
    public Integer getCooperacion() { return cooperacion; }
    public void setCooperacion(Integer cooperacion) { this.cooperacion = cooperacion; }
    public Integer getIniciativa() { return iniciativa; }
    public void setIniciativa(Integer iniciativa) { this.iniciativa = iniciativa; }
    public BigDecimal getPromedio() { return promedio; }
    public void setPromedio(BigDecimal promedio) { this.promedio = promedio; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public EstadoEvaluacion getEstado() { return estado; }
    public void setEstado(EstadoEvaluacion estado) { this.estado = estado; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
