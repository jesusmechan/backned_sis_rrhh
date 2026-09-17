package pe.andina.rrhh.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.Planilla;
import pe.andina.rrhh.domain.PlanillaDetalle;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class BoletaPdfService {

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color OK = new Color(4, 120, 87);
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final ParametroSistemaService parametros;

    public BoletaPdfService(ParametroSistemaService parametros) {
        this.parametros = parametros;
    }

    public byte[] exportarPlanilla(Planilla planilla, List<PlanillaDetalle> boletas) {
        if (boletas == null || boletas.isEmpty()) {
            throw ApiException.badRequest("No hay boletas. Calcule la planilla primero.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (int i = 0; i < boletas.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                dibujarBoleta(doc, planilla, boletas.get(i));
            }
            doc.close();
            return out.toByteArray();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.badRequest("No se pudo generar el PDF de boletas");
        }
    }

    public byte[] exportarBoleta(Planilla planilla, PlanillaDetalle detalle) {
        return exportarPlanilla(planilla, List.of(detalle));
    }

    private void dibujarBoleta(Document doc, Planilla planilla, PlanillaDetalle d) throws Exception {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        Font title = new Font(bf, 14, Font.BOLD, Color.WHITE);
        Font subtitle = new Font(bf, 9, Font.NORMAL, new Color(203, 213, 225));
        Font heading = new Font(bf, 11, Font.BOLD, NAVY);
        Font label = new Font(bf, 8, Font.BOLD, MUTED);
        Font body = new Font(bf, 9, Font.NORMAL, NAVY);
        Font money = new Font(bf, 9, Font.NORMAL, NAVY);
        Font moneyBold = new Font(bf, 11, Font.BOLD, Color.WHITE);
        Font small = new Font(bf, 8, Font.NORMAL, MUTED);

        String empresa = parametros.texto("empresa_razon_social", "Consultora Contable Andina S.A.C.");
        String ruc = parametros.texto("empresa_ruc", "20601234567");
        String periodo = periodo(planilla.getAnio(), planilla.getMes());
        Empleado e = d.getEmpleado();

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.2f, 1.4f});
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(NAVY);
        left.setBorderWidth(0);
        left.setPadding(14);
        left.addElement(new Paragraph(empresa, title));
        left.addElement(new Paragraph("RUC " + ruc, subtitle));
        left.addElement(new Paragraph("Boleta de pago", title));
        header.addCell(left);
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(NAVY);
        right.setBorderWidth(0);
        right.setPadding(14);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph periodoLbl = new Paragraph("Periodo", subtitle);
        periodoLbl.setAlignment(Element.ALIGN_RIGHT);
        Paragraph periodoVal = new Paragraph(periodo, title);
        periodoVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(periodoLbl);
        right.addElement(periodoVal);
        header.addCell(right);
        doc.add(header);
        doc.add(espacio(10));

        PdfPTable datos = new PdfPTable(4);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[]{1.1f, 1.6f, 1.1f, 1.6f});
        datos.addCell(kv("Trabajador", e.nombreCompleto(), label, body));
        datos.addCell(kv("Código", nz(e.getCodigoEmpleado()), label, body));
        datos.addCell(kv("Documento", docIdentidad(e), label, body));
        datos.addCell(kv("Modalidad", nz(d.getModalidad()), label, body));
        datos.addCell(kv("Área", e.getArea() != null ? e.getArea().getNombre() : "-", label, body));
        datos.addCell(kv("Cargo", e.getCargo() != null ? e.getCargo().getNombre() : "-", label, body));
        datos.addCell(kv("Estado planilla", planilla.getEstado() != null ? planilla.getEstado().name() : "-", label, body));
        datos.addCell(kv("Boleta", String.valueOf(d.getIdDetalle()), label, body));
        doc.add(datos);
        doc.add(espacio(12));

        PdfPTable cols = new PdfPTable(2);
        cols.setWidthPercentage(100);
        cols.setWidths(new float[]{1f, 1f});
        cols.addCell(seccion("Ingresos", List.of(
                fila("Remuneración básica", d.getRemuneracionBasica()),
                fila("Horas extras (" + n(d.getHorasExtras()) + " h)", d.getMontoHorasExtras()),
                fila("Total bruto", d.getBruto())
        ), heading, label, money, SURFACE));
        cols.addCell(seccion("Descuentos", List.of(
                fila("Ausencias (" + n(d.getDiasNoLaborados()) + " d)", d.getDescuentoAusencias()),
                fila("ONP 13%", d.getOnp()),
                fila("Total descuentos", nz(d.getOnp()).add(nz(d.getDescuentoAusencias())))
        ), heading, label, money, SURFACE));
        doc.add(cols);
        doc.add(espacio(10));

        PdfPTable neto = new PdfPTable(2);
        neto.setWidthPercentage(100);
        neto.setWidths(new float[]{2.2f, 1.2f});
        PdfPCell netoL = new PdfPCell();
        netoL.setBackgroundColor(OK);
        netoL.setBorderWidth(0);
        netoL.setPadding(12);
        netoL.addElement(new Paragraph("Neto a pagar", new Font(bf, 11, Font.BOLD, Color.WHITE)));
        netoL.addElement(new Paragraph("Aporte EsSalud (empleador): " + money(d.getEssalud()),
                new Font(bf, 8, Font.NORMAL, Color.WHITE)));
        PdfPCell netoR = box(money(d.getNeto()), moneyBold, Element.ALIGN_RIGHT, OK, 0);
        netoR.setPadding(12);
        netoR.setBorderWidth(0);
        neto.addCell(netoL);
        neto.addCell(netoR);
        doc.add(neto);
        doc.add(espacio(14));

        Paragraph nota = new Paragraph(
                "Documento generado por el Sistema de Gestión de RR. HH. Andina. "
                        + "Los montos corresponden al cálculo de planilla del periodo (básico, horas extras aprobadas, "
                        + "descuentos por ausencias, ONP y EsSalud). No sustituye la boleta SUNAT si el empleador debe emitirla.",
                small);
        nota.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(nota);
    }

    private PdfPCell seccion(String titulo, List<String[]> filas, Font heading, Font label, Font money, Color bg) {
        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{1.8f, 1f});
        PdfPCell title = box(titulo, heading, Element.ALIGN_LEFT, bg, 2);
        title.setPadding(8);
        inner.addCell(title);
        for (String[] f : filas) {
            inner.addCell(box(f[0], label, Element.ALIGN_LEFT, Color.WHITE, 0));
            inner.addCell(box(f[1], money, Element.ALIGN_RIGHT, Color.WHITE, 0));
        }
        PdfPCell wrap = new PdfPCell(inner);
        wrap.setPadding(4);
        wrap.setBorderColor(LINE);
        return wrap;
    }

    private PdfPCell kv(String k, String v, Font label, Font body) {
        PdfPTable t = new PdfPTable(1);
        t.addCell(box(k.toUpperCase(ES), label, Element.ALIGN_LEFT, SURFACE, 0));
        PdfPCell value = box(v, body, Element.ALIGN_LEFT, SURFACE, 0);
        value.setPaddingBottom(8);
        t.addCell(value);
        PdfPCell wrap = new PdfPCell(t);
        wrap.setBorderColor(LINE);
        wrap.setPadding(0);
        return wrap;
    }

    private PdfPCell box(String text, Font font, int align, Color bg, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setBorderColor(LINE);
        if (bg != null) {
            c.setBackgroundColor(bg);
        }
        if (colspan > 1) {
            c.setColspan(colspan);
        }
        return c;
    }

    private Paragraph espacio(float h) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(h);
        return p;
    }

    private String[] fila(String concepto, BigDecimal monto) {
        return new String[]{concepto, money(monto)};
    }

    private String money(BigDecimal value) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", s);
        return "S/ " + df.format(nz(value));
    }

    private String n(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nz(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String docIdentidad(Empleado e) {
        String tipo = e.getTipoDocumento() != null ? e.getTipoDocumento().name() : "DOC";
        return tipo + " " + nz(e.getNumeroDocumento());
    }

    private String periodo(Integer anio, Integer mes) {
        String nombre = Month.of(mes).getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + anio;
    }
}
