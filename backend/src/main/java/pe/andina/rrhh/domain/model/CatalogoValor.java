package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalogo_valor")
public class CatalogoValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor")
    private Integer idValor;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 20)
    private String tono;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "por_defecto", nullable = false)
    private Boolean porDefecto = false;

    @Column(length = 200)
    private String regla;

    @Column(name = "mensaje_regla", length = 200)
    private String mensajeRegla;

    @Column(nullable = false)
    private Boolean activo = true;

    public Integer getIdValor() { return idValor; }
    public void setIdValor(Integer idValor) { this.idValor = idValor; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTono() { return tono; }
    public void setTono(String tono) { this.tono = tono; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Boolean getPorDefecto() { return porDefecto; }
    public void setPorDefecto(Boolean porDefecto) { this.porDefecto = porDefecto; }
    public String getRegla() { return regla; }
    public void setRegla(String regla) { this.regla = regla; }
    public String getMensajeRegla() { return mensajeRegla; }
    public void setMensajeRegla(String mensajeRegla) { this.mensajeRegla = mensajeRegla; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
