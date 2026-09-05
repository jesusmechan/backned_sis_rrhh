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
import pe.andina.rrhh.domain.enums.ResultadoFilaCarga;

@Entity
@Table(name = "carga_masiva_detalle")
public class CargaMasivaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carga", nullable = false)
    private CargaMasiva carga;

    @Column(name = "numero_fila", nullable = false)
    private Integer numeroFila;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ResultadoFilaCarga resultado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @Column(length = 400)
    private String mensaje;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String datos;

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
    public CargaMasiva getCarga() { return carga; }
    public void setCarga(CargaMasiva carga) { this.carga = carga; }
    public Integer getNumeroFila() { return numeroFila; }
    public void setNumeroFila(Integer numeroFila) { this.numeroFila = numeroFila; }
    public ResultadoFilaCarga getResultado() { return resultado; }
    public void setResultado(ResultadoFilaCarga resultado) { this.resultado = resultado; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getDatos() { return datos; }
    public void setDatos(String datos) { this.datos = datos; }
}
