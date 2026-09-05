package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permiso_funcional")
public class PermisoFuncional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(length = 250)
    private String descripcion;

    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
