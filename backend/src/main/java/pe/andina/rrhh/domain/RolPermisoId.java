package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolPermisoId implements Serializable {

    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "id_permiso")
    private Integer idPermiso;

    public RolPermisoId() {}

    public RolPermisoId(Integer idRol, Integer idPermiso) {
        this.idRol = idRol;
        this.idPermiso = idPermiso;
    }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }
    public Integer getIdPermiso() { return idPermiso; }
    public void setIdPermiso(Integer idPermiso) { this.idPermiso = idPermiso; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolPermisoId that)) return false;
        return Objects.equals(idRol, that.idRol) && Objects.equals(idPermiso, that.idPermiso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol, idPermiso);
    }
}
