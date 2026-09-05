package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "horario_laboral")
public class HorarioLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer idHorario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "hora_ingreso", nullable = false)
    private LocalTime horaIngreso;

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "minutos_refrigerio", nullable = false)
    private Integer minutosRefrigerio = 60;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdHorario() { return idHorario; }
    public void setIdHorario(Integer idHorario) { this.idHorario = idHorario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalTime getHoraIngreso() { return horaIngreso; }
    public void setHoraIngreso(LocalTime horaIngreso) { this.horaIngreso = horaIngreso; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }
    public Integer getMinutosRefrigerio() { return minutosRefrigerio; }
    public void setMinutosRefrigerio(Integer minutosRefrigerio) { this.minutosRefrigerio = minutosRefrigerio; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
