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
import pe.andina.rrhh.domain.enums.FormatoReporte;
import pe.andina.rrhh.domain.enums.TipoReporte;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reporte_generado")
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TipoReporte tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FormatoReporte formato;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String filtros;

    @Column(name = "fecha_generacion", insertable = false, updatable = false)
    private OffsetDateTime fechaGeneracion;

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setTipo(TipoReporte tipo) { this.tipo = tipo; }
    public void setFormato(FormatoReporte formato) { this.formato = formato; }
    public void setFiltros(String filtros) { this.filtros = filtros; }
    public Integer getIdReporte() { return idReporte; }
}
