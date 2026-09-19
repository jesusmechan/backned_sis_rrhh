package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.CargaExcelUseCase;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.CargaMasiva;
import pe.andina.rrhh.domain.model.CargaMasivaDetalle;
import pe.andina.rrhh.domain.model.enums.EstadoCarga;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.ResultadoFilaCarga;
import pe.andina.rrhh.domain.model.enums.TipoContrato;
import pe.andina.rrhh.domain.model.enums.TipoDocumento;
import pe.andina.rrhh.application.dto.AppDtos.CargaFilaResponse;
import pe.andina.rrhh.application.dto.AppDtos.CargaResponse;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.CargaMasivaDetallePort;
import pe.andina.rrhh.application.port.out.CargaMasivaPort;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CargaExcelService implements CargaExcelUseCase {

    private final EmpleadoUseCase empleadoService;
    private final EmpleadoPort empleadoRepository;
    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final CargaMasivaPort cargaRepository;
    private final CargaMasivaDetallePort detalleRepository;
    private final Validator validator;

    private final CurrentUserPort currentUser;

    public CargaExcelService(EmpleadoUseCase empleadoService,
                             EmpleadoPort empleadoRepository,
                             AreaPort areaRepository,
                             CargoPort cargoRepository,
                             HorarioLaboralPort horarioRepository,
                             CargaMasivaPort cargaRepository,
                             CargaMasivaDetallePort detalleRepository,
                             Validator validator,
                           CurrentUserPort currentUser) {
        this.empleadoService = empleadoService;
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.cargaRepository = cargaRepository;
        this.detalleRepository = detalleRepository;
        this.validator = validator;
        this.currentUser = currentUser;
    }

    public byte[] plantilla() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("trabajadores");
            Row header = sheet.createRow(0);
            String[] cols = {
                    "codigo_empleado", "tipo_documento", "numero_documento", "nombres", "apellido_paterno",
                    "apellido_materno", "fecha_nacimiento", "sexo", "correo_institucional", "telefono",
                    "fecha_ingreso", "area", "cargo", "horario", "tipo_contrato", "estado"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw DomainException.badRequest("No se pudo generar la plantilla");
        }
    }

    @Transactional
    public CargaResponse cargar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw DomainException.badRequest("Debe adjuntar un archivo Excel");
        }
        CargaMasiva carga = new CargaMasiva();
        carga.setUsuario(currentUser.usuario());
        carga.setNombreArchivo(file.getOriginalFilename());
        carga.setEstado(EstadoCarga.PROCESANDO);
        cargaRepository.save(carga);

        DataFormatter fmt = new DataFormatter();
        int ok = 0;
        int fail = 0;
        int total = 0;
        List<CargaFilaResponse> filas = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                total++;
                try {
                    EmpleadoRequest request = mapRow(row, fmt);
                    validar(request);
                    boolean existe = empleadoRepository.findByCodigoEmpleado(request.codigoEmpleado()).isPresent();
                    if (existe) {
                        Integer id = empleadoRepository.findByCodigoEmpleado(request.codigoEmpleado()).orElseThrow().getIdEmpleado();
                        empleadoService.actualizar(id, request);
                        registrarDetalle(carga, i + 1, ResultadoFilaCarga.ACTUALIZADO, "Actualizado", filas);
                    } else {
                        empleadoService.crear(request);
                        registrarDetalle(carga, i + 1, ResultadoFilaCarga.INSERTADO, "Registrado", filas);
                    }
                    ok++;
                } catch (Exception ex) {
                    fail++;
                    registrarDetalle(carga, i + 1, ResultadoFilaCarga.ERROR, ex.getMessage(), filas);
                }
            }
        } catch (Exception ex) {
            carga.setEstado(EstadoCarga.FALLIDA);
            carga.setMensaje(ex.getMessage());
            carga.setFechaFin(OffsetDateTime.now());
            throw DomainException.badRequest("No se pudo leer el Excel: " + ex.getMessage());
        }
        carga.setTotalFilas(total);
        carga.setFilasExitosas(ok);
        carga.setFilasFallidas(fail);
        carga.setFechaFin(OffsetDateTime.now());
        carga.setEstado(fail == 0 ? EstadoCarga.COMPLETADA : EstadoCarga.COMPLETADA_CON_ERRORES);
        return new CargaResponse(carga.getIdCarga(), carga.getNombreArchivo(), total, ok, fail,
                carga.getEstado().name(), carga.getMensaje(), filas);
    }

    private void registrarDetalle(CargaMasiva carga, int fila, ResultadoFilaCarga resultado, String mensaje,
                                  List<CargaFilaResponse> filas) {
        CargaMasivaDetalle d = new CargaMasivaDetalle();
        d.setCarga(carga);
        d.setNumeroFila(fila);
        d.setResultado(resultado);
        d.setMensaje(mensaje);
        detalleRepository.save(d);
        filas.add(new CargaFilaResponse(fila, resultado.name(), mensaje));
    }

    private EmpleadoRequest mapRow(Row row, DataFormatter fmt) {
        String areaNombre = cell(row, 11, fmt);
        String cargoNombre = cell(row, 12, fmt);
        String horarioNombre = cell(row, 13, fmt);
        Integer idArea = areaRepository.findAll().stream()
                .filter(a -> a.getNombre().equalsIgnoreCase(areaNombre)).findFirst()
                .orElseThrow(() -> DomainException.badRequest("Área no existe: " + areaNombre)).getIdArea();
        Integer idCargo = cargoRepository.findAll().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(cargoNombre)).findFirst()
                .orElseThrow(() -> DomainException.badRequest("Cargo no existe: " + cargoNombre)).getIdCargo();
        Integer idHorario = horarioRepository.findAll().stream()
                .filter(h -> h.getNombre().equalsIgnoreCase(horarioNombre)).findFirst()
                .orElseThrow(() -> DomainException.badRequest("Horario no existe: " + horarioNombre)).getIdHorario();
        return new EmpleadoRequest(
                cell(row, 0, fmt).toUpperCase(),
                TipoDocumento.valueOf(orDefault(cell(row, 1, fmt), "DNI")),
                cell(row, 2, fmt).toUpperCase().replaceAll("[^A-Z0-9]", ""),
                cell(row, 3, fmt),
                cell(row, 4, fmt),
                cell(row, 5, fmt),
                parseDate(cell(row, 6, fmt)),
                null,
                cell(row, 8, fmt).toLowerCase(),
                null,
                cell(row, 9, fmt).replaceAll("\\D", ""),
                null,
                parseDate(cell(row, 10, fmt)),
                null,
                idArea, idCargo, idHorario,
                TipoContrato.valueOf(orDefault(cell(row, 14, fmt), "PLANILLA")),
                EstadoEmpleado.valueOf(orDefault(cell(row, 15, fmt), "ACTIVO")),
                null
        );
    }

    private void validar(EmpleadoRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<EmpleadoRequest> first = violations.iterator().next();
            throw DomainException.badRequest(first.getMessage());
        }
    }

    private String cell(Row row, int idx, DataFormatter fmt) {
        return row.getCell(idx) == null ? "" : fmt.formatCellValue(row.getCell(idx)).trim();
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
