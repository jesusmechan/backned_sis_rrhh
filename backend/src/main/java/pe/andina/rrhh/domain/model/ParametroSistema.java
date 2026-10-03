package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametro_sistema")
public class ParametroSistema {

    public static final String AMBITO_PUBLICO = "PUBLICO";
    public static final String AMBITO_SESION = "SESION";
    public static final String AMBITO_PRIVADO = "PRIVADO";

    @Id
    private String clave;

    @Column(nullable = false, length = 200)
    private String valor;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, length = 10)
    private String ambito = AMBITO_SESION;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getAmbito() { return ambito; }
    public void setAmbito(String ambito) { this.ambito = ambito; }
}
