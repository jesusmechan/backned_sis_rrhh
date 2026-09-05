package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Integer idArea;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 250)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
}
