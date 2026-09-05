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
import pe.andina.rrhh.domain.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.enums.SexoEmpleado;
import pe.andina.rrhh.domain.enums.TipoContrato;
import pe.andina.rrhh.domain.enums.TipoDocumento;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "codigo_empleado", nullable = false, length = 20)
    private String codigoEmpleado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento = TipoDocumento.DNI;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false, length = 80)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 80)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SexoEmpleado sexo;

    @Column(name = "correo_institucional", nullable = false, length = 120)
    private String correoInstitucional;

    @Column(name = "correo_personal", length = 120)
    private String correoPersonal;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_cese")
    private LocalDate fechaCese;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_area", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario", nullable = false)
    private HorarioLaboral horario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_contrato", nullable = false)
    private TipoContrato tipoContrato = TipoContrato.PLANILLA;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jefe_inmediato")
    private Empleado jefeInmediato;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public String nombreCompleto() {
        return String.join(" ", nombres, apellidoPaterno, apellidoMaterno).trim();
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }
    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public SexoEmpleado getSexo() { return sexo; }
    public void setSexo(SexoEmpleado sexo) { this.sexo = sexo; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { this.correoInstitucional = correoInstitucional; }
    public String getCorreoPersonal() { return correoPersonal; }
    public void setCorreoPersonal(String correoPersonal) { this.correoPersonal = correoPersonal; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public LocalDate getFechaCese() { return fechaCese; }
    public void setFechaCese(LocalDate fechaCese) { this.fechaCese = fechaCese; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public HorarioLaboral getHorario() { return horario; }
    public void setHorario(HorarioLaboral horario) { this.horario = horario; }
    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }
    public EstadoEmpleado getEstado() { return estado; }
    public void setEstado(EstadoEmpleado estado) { this.estado = estado; }
    public Empleado getJefeInmediato() { return jefeInmediato; }
    public void setJefeInmediato(Empleado jefeInmediato) { this.jefeInmediato = jefeInmediato; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
}
