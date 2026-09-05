package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rol_permiso")
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id = new RolPermisoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRol")
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPermiso")
    @JoinColumn(name = "id_permiso")
    private PermisoFuncional permiso;

    @Column(name = "fecha_asignacion", insertable = false, updatable = false)
    private OffsetDateTime fechaAsignacion;

    public RolPermisoId getId() { return id; }
    public void setId(RolPermisoId id) { this.id = id; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public PermisoFuncional getPermiso() { return permiso; }
    public void setPermiso(PermisoFuncional permiso) { this.permiso = permiso; }
}
