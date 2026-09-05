package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_permiso")
public class TipoPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_permiso")
    private Integer idTipoPermiso;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "requiere_sustento", nullable = false)
    private Boolean requiereSustento = false;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdTipoPermiso() { return idTipoPermiso; }
    public void setIdTipoPermiso(Integer idTipoPermiso) { this.idTipoPermiso = idTipoPermiso; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getRequiereSustento() { return requiereSustento; }
    public void setRequiereSustento(Boolean requiereSustento) { this.requiereSustento = requiereSustento; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
