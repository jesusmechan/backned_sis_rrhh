package pe.andina.rrhh.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "planilla_detalle")
public class PlanillaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planilla", nullable = false)
    private Planilla planilla;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato")
    private Contrato contrato;

    @Column(length = 20)
    private String modalidad;

    @Column(name = "regimen_pensionario", length = 20)
    private String regimenPensionario;

    @Column(name = "remuneracion_basica", nullable = false, precision = 12, scale = 2)
    private BigDecimal remuneracionBasica = BigDecimal.ZERO;

    @Column(name = "asignacion_familiar", nullable = false, precision = 12, scale = 2)
    private BigDecimal asignacionFamiliar = BigDecimal.ZERO;

    @Column(name = "horas_extras", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasExtras = BigDecimal.ZERO;

    @Column(name = "monto_horas_extras", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoHorasExtras = BigDecimal.ZERO;

    @Column(name = "dias_computados", nullable = false, precision = 6, scale = 2)
    private BigDecimal diasComputados = BigDecimal.valueOf(30);

    @Column(name = "dias_no_laborados", nullable = false, precision = 6, scale = 2)
    private BigDecimal diasNoLaborados = BigDecimal.ZERO;

    @Column(name = "descuento_ausencias", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAusencias = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal onp = BigDecimal.ZERO;

    @Column(name = "afp_aporte", nullable = false, precision = 12, scale = 2)
    private BigDecimal afpAporte = BigDecimal.ZERO;

    @Column(name = "afp_comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal afpComision = BigDecimal.ZERO;

    @Column(name = "afp_seguro", nullable = false, precision = 12, scale = 2)
    private BigDecimal afpSeguro = BigDecimal.ZERO;

    @Column(name = "quinta_categoria", nullable = false, precision = 12, scale = 2)
    private BigDecimal quintaCategoria = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal essalud = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bruto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal neto = BigDecimal.ZERO;

    @Column(name = "gratificacion_proyectada", nullable = false, precision = 12, scale = 2)
    private BigDecimal gratificacionProyectada = BigDecimal.ZERO;

    @Column(name = "cts_proyectado", nullable = false, precision = 12, scale = 2)
    private BigDecimal ctsProyectado = BigDecimal.ZERO;

    @Column(name = "regimen_laboral", length = 20)
    private String regimenLaboral;

    public BigDecimal totalAfp() {
        return nz(afpAporte).add(nz(afpComision)).add(nz(afpSeguro));
    }

    public BigDecimal totalPension() {
        return nz(onp).add(totalAfp());
    }

    public BigDecimal totalDescuentosTrabajador() {
        return totalPension().add(nz(descuentoAusencias)).add(nz(quintaCategoria));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
    public Planilla getPlanilla() { return planilla; }
    public void setPlanilla(Planilla planilla) { this.planilla = planilla; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getRegimenPensionario() { return regimenPensionario; }
    public void setRegimenPensionario(String regimenPensionario) { this.regimenPensionario = regimenPensionario; }
    public BigDecimal getRemuneracionBasica() { return remuneracionBasica; }
    public void setRemuneracionBasica(BigDecimal remuneracionBasica) { this.remuneracionBasica = remuneracionBasica; }
    public BigDecimal getAsignacionFamiliar() { return asignacionFamiliar; }
    public void setAsignacionFamiliar(BigDecimal asignacionFamiliar) { this.asignacionFamiliar = asignacionFamiliar; }
    public BigDecimal getHorasExtras() { return horasExtras; }
    public void setHorasExtras(BigDecimal horasExtras) { this.horasExtras = horasExtras; }
    public BigDecimal getMontoHorasExtras() { return montoHorasExtras; }
    public void setMontoHorasExtras(BigDecimal montoHorasExtras) { this.montoHorasExtras = montoHorasExtras; }
    public BigDecimal getDiasComputados() { return diasComputados; }
    public void setDiasComputados(BigDecimal diasComputados) { this.diasComputados = diasComputados; }
    public BigDecimal getDiasNoLaborados() { return diasNoLaborados; }
    public void setDiasNoLaborados(BigDecimal diasNoLaborados) { this.diasNoLaborados = diasNoLaborados; }
    public BigDecimal getDescuentoAusencias() { return descuentoAusencias; }
    public void setDescuentoAusencias(BigDecimal descuentoAusencias) { this.descuentoAusencias = descuentoAusencias; }
    public BigDecimal getOnp() { return onp; }
    public void setOnp(BigDecimal onp) { this.onp = onp; }
    public BigDecimal getAfpAporte() { return afpAporte; }
    public void setAfpAporte(BigDecimal afpAporte) { this.afpAporte = afpAporte; }
    public BigDecimal getAfpComision() { return afpComision; }
    public void setAfpComision(BigDecimal afpComision) { this.afpComision = afpComision; }
    public BigDecimal getAfpSeguro() { return afpSeguro; }
    public void setAfpSeguro(BigDecimal afpSeguro) { this.afpSeguro = afpSeguro; }
    public BigDecimal getQuintaCategoria() { return quintaCategoria; }
    public void setQuintaCategoria(BigDecimal quintaCategoria) { this.quintaCategoria = quintaCategoria; }
    public BigDecimal getEssalud() { return essalud; }
    public void setEssalud(BigDecimal essalud) { this.essalud = essalud; }
    public BigDecimal getBruto() { return bruto; }
    public void setBruto(BigDecimal bruto) { this.bruto = bruto; }
    public BigDecimal getNeto() { return neto; }
    public void setNeto(BigDecimal neto) { this.neto = neto; }
    public BigDecimal getGratificacionProyectada() { return gratificacionProyectada; }
    public void setGratificacionProyectada(BigDecimal gratificacionProyectada) { this.gratificacionProyectada = gratificacionProyectada; }
    public BigDecimal getCtsProyectado() { return ctsProyectado; }
    public void setCtsProyectado(BigDecimal ctsProyectado) { this.ctsProyectado = ctsProyectado; }
    public String getRegimenLaboral() { return regimenLaboral; }
    public void setRegimenLaboral(String regimenLaboral) { this.regimenLaboral = regimenLaboral; }
}
