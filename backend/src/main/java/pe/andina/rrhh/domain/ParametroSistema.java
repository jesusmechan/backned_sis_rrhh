package pe.andina.rrhh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametro_sistema")
public class ParametroSistema {

    @Id
    private String clave;

    @Column(nullable = false, length = 200)
    private String valor;

    @Column(length = 300)
    private String descripcion;

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public String getDescripcion() { return descripcion; }
}
