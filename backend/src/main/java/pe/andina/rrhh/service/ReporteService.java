package pe.andina.rrhh.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.ReporteGenerado;
import pe.andina.rrhh.domain.enums.FormatoReporte;
import pe.andina.rrhh.domain.enums.TipoReporte;
import pe.andina.rrhh.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.repo.ReporteGeneradoRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ReporteService {

    private final EmpleadoService empleadoService;
    private final UsuarioService usuarioService;
    private final SolicitudService solicitudService;
    private final AsistenciaService asistenciaService;
    private final ReporteGeneradoRepository reporteRepository;

    public ReporteService(EmpleadoService empleadoService,
                          UsuarioService usuarioService,
                          SolicitudService solicitudService,
                          AsistenciaService asistenciaService,
                          ReporteGeneradoRepository reporteRepository) {
        this.empleadoService = empleadoService;
        this.usuarioService = usuarioService;
        this.solicitudService = solicitudService;
        this.asistenciaService = asistenciaService;
        this.reporteRepository = reporteRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> trabajadores() { return empleadoService.listar(); }

    @Transactional(readOnly = true)
    public List<PermisoResponse> permisos() { return solicitudService.listarPermisos(); }

    @Transactional(readOnly = true)
    public List<HoraExtraResponse> horasExtras() { return solicitudService.listarHorasExtras(); }

    @Transactional(readOnly = true)
    public List<MarcacionResponse> asistencia() {
        return asistenciaService.reporte(null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MarcacionResponse> asistencia(Integer idEmpleado, java.time.LocalDate desde, java.time.LocalDate hasta) {
        return asistenciaService.reporte(idEmpleado, desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> usuarios() { return usuarioService.listar(); }

    @Transactional
    public byte[] exportar(TipoReporte tipo, FormatoReporte formato) {
        registrar(tipo, formato);
        List<String[]> rows = filas(tipo);
        return formato == FormatoReporte.PDF ? toPdf(tipo.name(), rows) : toExcel(tipo.name(), rows);
    }

    private void registrar(TipoReporte tipo, FormatoReporte formato) {
        ReporteGenerado r = new ReporteGenerado();
        r.setUsuario(SecurityUtils.current().getUsuario());
        r.setTipo(tipo);
        r.setFormato(formato);
        reporteRepository.save(r);
    }

    private List<String[]> filas(TipoReporte tipo) {
        return switch (tipo) {
            case TRABAJADORES -> {
                List<String[]> data = new java.util.ArrayList<>();
                    data.add(new String[]{"Código", "Nombre", "Área", "Cargo", "Contrato", "Estado"});
                for (EmpleadoResponse e : empleadoService.listar()) {
                    data.add(new String[]{e.codigoEmpleado(), e.nombreCompleto(), e.area(), e.cargo(),
                            e.tipoContrato() != null ? e.tipoContrato().name() : "", e.estado().name()});
                }
                yield data;
            }
            case PERMISOS -> {
                List<String[]> data = new java.util.ArrayList<>();
                data.add(new String[]{"Id", "Empleado", "Tipo", "Inicio", "Fin", "Estado"});
                for (PermisoResponse p : solicitudService.listarPermisos()) {
                    data.add(new String[]{String.valueOf(p.idSolicitudPermiso()), p.empleado(), p.tipoPermiso(),
                            String.valueOf(p.fechaInicio()), String.valueOf(p.fechaFin()), p.estado().name()});
                }
                yield data;
            }
            case HORAS_EXTRAS -> {
                List<String[]> data = new java.util.ArrayList<>();
                data.add(new String[]{"Id", "Empleado", "Fecha", "Horas", "Estado"});
                for (HoraExtraResponse h : solicitudService.listarHorasExtras()) {
                    data.add(new String[]{String.valueOf(h.idSolicitudHoraExtra()), h.empleado(),
                            String.valueOf(h.fecha()), String.valueOf(h.cantidadHoras()), h.estado().name()});
                }
                yield data;
            }
            case ASISTENCIA -> {
                List<String[]> data = new java.util.ArrayList<>();
                data.add(new String[]{"Id", "Empleado", "Tipo", "Fecha", "Hora", "Origen"});
                for (MarcacionResponse m : asistenciaService.reporte(null, null, null)) {
                    data.add(new String[]{String.valueOf(m.idMarcacion()), m.empleado(), m.tipo().name(),
                            String.valueOf(m.fecha()), String.valueOf(m.fechaHora()), m.origen()});
                }
                yield data;
            }
            case USUARIOS -> {
                List<String[]> data = new java.util.ArrayList<>();
                data.add(new String[]{"Usuario", "Correo", "Rol", "Activo"});
                for (UsuarioResponse u : usuarioService.listar()) {
                    data.add(new String[]{u.nombreUsuario(), u.correo(), u.rol(), String.valueOf(u.activo())});
                }
                yield data;
            }
        };
    }

    private byte[] toExcel(String sheetName, List<String[]> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i);
                String[] cols = rows.get(i);
                for (int c = 0; c < cols.length; c++) {
                    row.createCell(c).setCellValue(cols[c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw ApiException.badRequest("No se pudo generar el Excel");
        }
    }

    private byte[] toPdf(String titulo, List<String[]> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Reporte " + titulo));
            if (!rows.isEmpty()) {
                PdfPTable table = new PdfPTable(rows.getFirst().length);
                for (String[] row : rows) {
                    for (String col : row) {
                        table.addCell(col == null ? "" : col);
                    }
                }
                doc.add(table);
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw ApiException.badRequest("No se pudo generar el PDF");
        }
    }
}
