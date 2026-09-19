package pe.andina.rrhh.adapter.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.out.BoletaPdfPort;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Planilla;
import pe.andina.rrhh.domain.model.PlanillaDetalle;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class BoletaPdfService implements BoletaPdfPort {

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color INK = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color RULE = new Color(203, 213, 225);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color GOLD = new Color(180, 138, 60);
    private static final Color WHITE = Color.WHITE;
    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String[] UNIDADES = {
            "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };
    private static final String[] DECENAS = {
            "", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private final ParametroPort parametros;

    public BoletaPdfService(ParametroPort parametros) {
        this.parametros = parametros;
    }

    public byte[] exportarPlanilla(Planilla planilla, List<PlanillaDetalle> boletas) {
        if (boletas == null || boletas.isEmpty()) {
            throw DomainException.badRequest("No hay boletas. Calcule la planilla primero.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 36, 48);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new MarcoPagina());
            doc.open();
            for (int i = 0; i < boletas.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                dibujarBoleta(doc, planilla, boletas.get(i));
            }
            doc.close();
            return out.toByteArray();
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw DomainException.badRequest("No se pudo generar el PDF de boletas");
        }
    }

    public byte[] exportarBoleta(Planilla planilla, PlanillaDetalle detalle) {
        return exportarPlanilla(planilla, List.of(detalle));
    }

    private void dibujarBoleta(Document doc, Planilla planilla, PlanillaDetalle d) throws Exception {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        Fonts f = new Fonts(bf);
        Empleado e = d.getEmpleado();
        String empresa = parametros.texto("empresa_razon_social", "Consultora Contable Andina S.A.C.");
        String ruc = parametros.texto("empresa_ruc", "20601234567");
        String direccion = parametros.texto("empresa_direccion", "Av. Javier Prado 1200, San Isidro, Lima");
        String periodo = periodo(planilla.getAnio(), planilla.getMes());
        String nro = String.format("AND-%d-%02d-%04d", planilla.getAnio(), planilla.getMes(), d.getIdDetalle());
        BigDecimal totalDesc = nz(d.getOnp()).add(nz(d.getDescuentoAusencias()));
        String onpPct = porcentaje(parametros.decimal("tasa_onp", "0.13"));
        String essaludPct = porcentaje(parametros.decimal("tasa_essalud", "0.09"));

        doc.add(membrete(empresa, ruc, direccion, periodo, nro, f));
        doc.add(espacio(10));
        doc.add(seccionTitulo("Datos del trabajador", f));
        doc.add(fichaTrabajador(e, d, f));
        doc.add(espacio(12));

        PdfPTable cols = new PdfPTable(2);
        cols.setWidthPercentage(100);
        cols.setWidths(new float[]{1f, 1f});
        cols.setSpacingAfter(10);
        cols.addCell(wrap(tablaConceptos("Ingresos", List.of(
                fila("Remuneración básica", d.getRemuneracionBasica()),
                fila("Horas extras (" + n(d.getHorasExtras()) + " h)", d.getMontoHorasExtras())
        ), "Total ingresos", d.getBruto(), f), 0, 6));
        cols.addCell(wrap(tablaConceptos("Descuentos", List.of(
                fila("Ausencias (" + n(d.getDiasNoLaborados()) + " d)", d.getDescuentoAusencias()),
                fila("ONP " + onpPct, d.getOnp())
        ), "Total descuentos", totalDesc, f), 6, 0));
        doc.add(cols);

        doc.add(aportesEmpleador(d, essaludPct, f));
        doc.add(espacio(10));
        doc.add(cajaNeto(d.getNeto(), f));
        doc.add(espacio(16));
        doc.add(firmas(empresa, e.nombreCompleto(), f));
        doc.add(espacio(10));

        Paragraph nota = new Paragraph(
                "Documento interno del Sistema de Gestión de RR. HH. Andina. "
                        + "Montos del periodo: remuneración básica, horas extras aprobadas, descuento por ausencias y ONP. "
                        + "EsSalud es aporte del empleador y no se descuenta al trabajador. "
                        + "No sustituye la boleta electrónica SUNAT si el empleador debe emitirla.",
                f.tiny);
        nota.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(nota);
    }

    private PdfPTable membrete(String empresa, String ruc, String direccion, String periodo, String nro, Fonts f) {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.2f, 1.55f});

        PdfPTable brand = new PdfPTable(2);
        brand.setWidths(new float[]{0.42f, 2.8f});
        PdfPCell logo = new PdfPCell(new Phrase("CA", f.logo));
        logo.setBackgroundColor(NAVY);
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setPaddingTop(10);
        logo.setPaddingBottom(10);
        logo.setFixedHeight(44);
        brand.addCell(logo);

        PdfPCell info = new PdfPCell();
        info.setBorder(Rectangle.NO_BORDER);
        info.setPaddingLeft(10);
        info.setVerticalAlignment(Element.ALIGN_MIDDLE);
        info.addElement(new Paragraph(empresa, f.company));
        info.addElement(new Paragraph("RUC " + ruc, f.meta));
        info.addElement(new Paragraph(direccion, f.meta));
        brand.addCell(info);

        PdfPCell left = new PdfPCell(brand);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPaddingRight(12);
        header.addCell(left);

        PdfPTable box = new PdfPTable(1);
        PdfPCell tipo = cell("BOLETA DE PAGO", f.docTitle, Element.ALIGN_CENTER, NAVY, Rectangle.NO_BORDER);
        tipo.setPaddingTop(8);
        tipo.setPaddingBottom(4);
        box.addCell(tipo);
        PdfPCell per = cell(periodo, f.docPeriod, Element.ALIGN_CENTER, NAVY, Rectangle.NO_BORDER);
        per.setPaddingBottom(2);
        box.addCell(per);
        PdfPCell num = cell(nro, f.docMeta, Element.ALIGN_CENTER, NAVY, Rectangle.NO_BORDER);
        num.setPaddingBottom(8);
        box.addCell(num);
        PdfPCell right = new PdfPCell(box);
        right.setBorderColor(NAVY);
        right.setBorderWidth(1.1f);
        right.setBackgroundColor(NAVY);
        header.addCell(right);
        return header;
    }

    private PdfPTable fichaTrabajador(Empleado e, PlanillaDetalle d, Fonts f) {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.7f, 1f, 1.15f, 1.05f});
        dato(t, "Apellidos y nombres", e.nombreCompleto(), f, 2);
        dato(t, "Código", nz(e.getCodigoEmpleado()), f, 1);
        dato(t, "Documento", docIdentidad(e), f, 1);
        dato(t, "Área", e.getArea() != null ? e.getArea().getNombre() : "-", f, 1);
        dato(t, "Cargo", e.getCargo() != null ? e.getCargo().getNombre() : "-", f, 1);
        dato(t, "Fecha de ingreso", fecha(e.getFechaIngreso()), f, 1);
        dato(t, "Régimen / modalidad", etiqueta(e.getTipoContrato()) + " / " + etiqueta(d.getModalidad()), f, 1);
        return t;
    }

    private void dato(PdfPTable t, String k, String v, Fonts f, int colspan) {
        PdfPCell c = new PdfPCell();
        c.setColspan(colspan);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.6f);
        c.setBackgroundColor(SURFACE);
        c.setPadding(7);
        c.setPaddingTop(6);
        c.addElement(new Paragraph(k.toUpperCase(ES), f.kicker));
        Paragraph value = new Paragraph(v, f.body);
        value.setSpacingBefore(1.5f);
        c.addElement(value);
        t.addCell(c);
    }

    private PdfPTable tablaConceptos(String titulo, List<String[]> filas, String totalLbl, BigDecimal total, Fonts f) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.85f, 1f});
        PdfPCell head = cell(titulo.toUpperCase(ES), f.colHead, Element.ALIGN_LEFT, NAVY, Rectangle.NO_BORDER);
        head.setColspan(2);
        head.setPadding(8);
        t.addCell(head);
        t.addCell(th("Concepto", Element.ALIGN_LEFT, f));
        t.addCell(th("Importe", Element.ALIGN_RIGHT, f));
        for (String[] fila : filas) {
            t.addCell(td(fila[0], Element.ALIGN_LEFT, f.label, WHITE, false));
            t.addCell(td(fila[1], Element.ALIGN_RIGHT, f.money, WHITE, false));
        }
        t.addCell(td(totalLbl, Element.ALIGN_LEFT, f.totalLbl, SURFACE, true));
        t.addCell(td(money(total), Element.ALIGN_RIGHT, f.totalVal, SURFACE, true));
        return t;
    }

    private PdfPTable aportesEmpleador(PlanillaDetalle d, String essaludPct, Fonts f) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{3.2f, 1.2f});
        PdfPCell title = new PdfPCell();
        title.setBackgroundColor(SURFACE);
        title.setBorderColor(LINE);
        title.setBorderWidth(0.7f);
        title.setPadding(8);
        title.addElement(new Paragraph("Aportes del empleador", f.section));
        title.addElement(new Paragraph("EsSalud " + essaludPct + "  ·  no se descuenta al trabajador", f.meta));
        t.addCell(title);
        PdfPCell monto = cell(money(d.getEssalud()), f.totalVal, Element.ALIGN_RIGHT, SURFACE, Rectangle.BOX);
        monto.setBorderColor(LINE);
        monto.setPadding(8);
        monto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(monto);
        return t;
    }

    private PdfPTable cajaNeto(BigDecimal neto, Fonts f) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{2.15f, 1.15f});
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(NAVY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(12);
        left.addElement(new Paragraph("NETO A PAGAR", f.netoLbl));
        Paragraph son = new Paragraph("Son: " + enLetras(neto), f.netoWords);
        son.setSpacingBefore(3);
        left.addElement(son);
        t.addCell(left);
        PdfPCell right = cell(money(neto), f.netoAmt, Element.ALIGN_RIGHT, NAVY, Rectangle.NO_BORDER);
        right.setPadding(12);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(right);
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        PdfPCell gold = new PdfPCell(t);
        gold.setBorderColor(GOLD);
        gold.setBorderWidth(1.6f);
        gold.setPadding(2);
        wrap.addCell(gold);
        return wrap;
    }

    private PdfPTable firmas(String empresa, String trabajador, Fonts f) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});
        t.addCell(firmaCol("Empleador", empresa, f));
        t.addCell(firmaCol("Trabajador", trabajador, f));
        return t;
    }

    private PdfPCell firmaCol(String rol, String nombre, Fonts f) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph line = new Paragraph("______________________________", f.meta);
        line.setAlignment(Element.ALIGN_CENTER);
        Paragraph who = new Paragraph(nombre, f.body);
        who.setAlignment(Element.ALIGN_CENTER);
        who.setSpacingBefore(4);
        Paragraph role = new Paragraph(rol.toUpperCase(ES), f.kicker);
        role.setAlignment(Element.ALIGN_CENTER);
        role.setSpacingBefore(2);
        c.addElement(line);
        c.addElement(who);
        c.addElement(role);
        return c;
    }

    private Paragraph seccionTitulo(String texto, Fonts f) {
        Paragraph p = new Paragraph(texto.toUpperCase(ES), f.section);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPCell wrap(PdfPTable inner, float left, float right) {
        PdfPCell c = new PdfPCell(inner);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingLeft(left);
        c.setPaddingRight(right);
        return c;
    }

    private PdfPCell th(String text, int align, Fonts f) {
        PdfPCell c = cell(text, f.th, align, SURFACE, Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPaddingTop(7);
        c.setPaddingBottom(6);
        return c;
    }

    private PdfPCell td(String text, int align, Font font, Color bg, boolean total) {
        PdfPCell c = cell(text, font, align, bg, total ? Rectangle.TOP : Rectangle.BOTTOM);
        c.setBorderColor(total ? NAVY : LINE);
        c.setBorderWidth(total ? 0.9f : 0.4f);
        c.setPaddingTop(total ? 8 : 6);
        c.setPaddingBottom(total ? 8 : 6);
        return c;
    }

    private PdfPCell cell(String text, Font font, int align, Color bg, int border) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        c.setBorder(border);
        c.setBorderColor(LINE);
        if (bg != null) {
            c.setBackgroundColor(bg);
        }
        return c;
    }

    private Paragraph espacio(float h) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(h);
        p.setSpacingAfter(0);
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

    private String porcentaje(BigDecimal tasa) {
        BigDecimal pct = nz(tasa).multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
        return pct.toPlainString() + "%";
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nz(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String etiqueta(Enum<?> value) {
        return value == null ? "-" : etiqueta(value.name());
    }

    private String etiqueta(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String raw = value.replace('_', ' ').toLowerCase(ES);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String docIdentidad(Empleado e) {
        String tipo = e.getTipoDocumento() != null ? e.getTipoDocumento().name() : "DOC";
        return tipo + " " + nz(e.getNumeroDocumento());
    }

    private String fecha(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String periodo(Integer anio, Integer mes) {
        String nombre = Month.of(mes).getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + anio;
    }

    private String enLetras(BigDecimal value) {
        BigDecimal n = nz(value).abs().setScale(2, RoundingMode.HALF_UP);
        long enteros = n.longValue();
        int centavos = n.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String letras = enteros == 0 ? "CERO" : convertir(enteros);
        String moneda = enteros == 1 ? "SOL" : "SOLES";
        return letras + " CON " + String.format("%02d", centavos) + "/100 " + moneda;
    }

    private String convertir(long n) {
        if (n == 0) {
            return "CERO";
        }
        if (n < 0 || n > 999_999_999L) {
            return money(BigDecimal.valueOf(n)).replace("S/ ", "");
        }
        StringBuilder out = new StringBuilder();
        int millones = (int) (n / 1_000_000);
        int miles = (int) ((n % 1_000_000) / 1_000);
        int resto = (int) (n % 1_000);
        if (millones > 0) {
            out.append(millones == 1 ? "UN MILLON" : convertirGrupo(millones) + " MILLONES");
        }
        if (miles > 0) {
            if (!out.isEmpty()) {
                out.append(" ");
            }
            out.append(miles == 1 ? "MIL" : convertirGrupo(miles) + " MIL");
        }
        if (resto > 0) {
            if (!out.isEmpty()) {
                out.append(" ");
            }
            out.append(convertirGrupo(resto));
        }
        return out.toString();
    }

    private String convertirGrupo(int n) {
        if (n == 100) {
            return "CIEN";
        }
        if (n <= 20) {
            return UNIDADES[n];
        }
        int c = n / 100;
        int d = (n % 100) / 10;
        int u = n % 10;
        int du = n % 100;
        StringBuilder out = new StringBuilder();
        if (c > 0) {
            out.append(CENTENAS[c]);
        }
        if (du > 0) {
            if (!out.isEmpty()) {
                out.append(" ");
            }
            if (du <= 20) {
                out.append(UNIDADES[du]);
            } else if (du < 30) {
                out.append(u == 0 ? "VEINTE" : "VEINTI" + UNIDADES[u].replace("UN", "UNO"));
            } else {
                out.append(DECENAS[d]);
                if (u == 1) {
                    out.append(" Y UN");
                } else if (u > 0) {
                    out.append(" Y ").append(UNIDADES[u]);
                }
            }
        }
        return out.toString();
    }

    private static final class Fonts {
        final Font logo;
        final Font company;
        final Font meta;
        final Font docTitle;
        final Font docPeriod;
        final Font docMeta;
        final Font section;
        final Font kicker;
        final Font body;
        final Font label;
        final Font money;
        final Font th;
        final Font colHead;
        final Font totalLbl;
        final Font totalVal;
        final Font netoLbl;
        final Font netoWords;
        final Font netoAmt;
        final Font tiny;

        Fonts(BaseFont bf) {
            logo = new Font(bf, 13, Font.BOLD, WHITE);
            company = new Font(bf, 11, Font.BOLD, NAVY);
            meta = new Font(bf, 8, Font.NORMAL, MUTED);
            docTitle = new Font(bf, 10, Font.BOLD, WHITE);
            docPeriod = new Font(bf, 11, Font.BOLD, WHITE);
            docMeta = new Font(bf, 7.5f, Font.NORMAL, new Color(226, 232, 240));
            section = new Font(bf, 8.5f, Font.BOLD, NAVY);
            kicker = new Font(bf, 6.5f, Font.BOLD, MUTED);
            body = new Font(bf, 9, Font.NORMAL, INK);
            label = new Font(bf, 8.5f, Font.NORMAL, INK);
            money = new Font(bf, 8.5f, Font.NORMAL, INK);
            th = new Font(bf, 7, Font.BOLD, MUTED);
            colHead = new Font(bf, 8.5f, Font.BOLD, WHITE);
            totalLbl = new Font(bf, 8.5f, Font.BOLD, NAVY);
            totalVal = new Font(bf, 9, Font.BOLD, NAVY);
            netoLbl = new Font(bf, 9, Font.BOLD, WHITE);
            netoWords = new Font(bf, 7.5f, Font.NORMAL, new Color(203, 213, 225));
            netoAmt = new Font(bf, 16, Font.BOLD, WHITE);
            tiny = new Font(bf, 7, Font.NORMAL, MUTED);
        }
    }

    private static final class MarcoPagina extends PdfPageEventHelper {
        private BaseFont bf;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {
                bf = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w = document.getPageSize().getWidth();
            float h = document.getPageSize().getHeight();
            cb.saveState();
            cb.setColorFill(NAVY);
            cb.rectangle(0, h - 7, w, 7);
            cb.fill();
            cb.setColorFill(GOLD);
            cb.rectangle(0, h - 9, w, 2);
            cb.fill();
            cb.setColorStroke(LINE);
            cb.setLineWidth(0.6f);
            cb.moveTo(document.left(), 34);
            cb.lineTo(document.right(), 34);
            cb.stroke();
            if (bf != null) {
                cb.beginText();
                cb.setFontAndSize(bf, 7);
                cb.setColorFill(MUTED);
                String emitido = ZonedDateTime.now(LIMA)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ES));
                cb.showTextAligned(Element.ALIGN_LEFT, "Emitido " + emitido + "  |  Consultora Andina", document.left(), 22, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Página " + writer.getPageNumber(), document.right(), 22, 0);
                cb.endText();
            }
            cb.restoreState();
        }
    }
}
