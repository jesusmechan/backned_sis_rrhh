package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import pe.andina.rrhh.domain.model.enums.SexoEmpleado;
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
import pe.andina.rrhh.application.port.out.ExcelPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CargaExcelService implements CargaExcelUseCase {

    private static final Logger log = LoggerFactory.getLogger(CargaExcelService.class);

    private static final ImmutableList<String> COLUMNAS = ImmutableList.of(
            "codigo_empleado", "tipo_documento", "numero_documento", "nombres", "apellido_paterno",
            "apellido_materno", "fecha_nacimiento", "sexo", "correo_institucional", "telefono",
            "fecha_ingreso", "area", "cargo", "horario", "tipo_contrato", "estado"
    );

    private final EmpleadoUseCase empleadoService;
    private final EmpleadoPort empleadoRepository;
    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final CargaMasivaPort cargaRepository;
    private final CargaMasivaDetallePort detalleRepository;
    private final Validator validator;
    private final ExcelPort excelPort;

    private final CurrentUserPort currentUser;

    public CargaExcelService(EmpleadoUseCase empleadoService,
                             EmpleadoPort empleadoRepository,
                             AreaPort areaRepository,
                             CargoPort cargoRepository,
                             HorarioLaboralPort horarioRepository,
                             CargaMasivaPort cargaRepository,
                             CargaMasivaDetallePort detalleRepository,
                             Validator validator,
                             ExcelPort excelPort,
                           CurrentUserPort currentUser) {
        this.empleadoService = empleadoService;
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.cargaRepository = cargaRepository;
        this.detalleRepository = detalleRepository;
        this.validator = validator;
        this.excelPort = excelPort;
        this.currentUser = currentUser;
    }

    public byte[] plantilla() {
        log.info("Generando plantilla Excel de trabajadores");
        return excelPort.escribir("trabajadores", ImmutableList.of(COLUMNAS.toArray(String[]::new)));
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
        log.info("Carga Excel iniciada archivo={} idCarga={}", file.getOriginalFilename(), carga.getIdCarga());

        int ok = 0;
        int fail = 0;
        int total = 0;
        List<CargaFilaResponse> filas = new ArrayList<>();
        try (InputStream in = file.getInputStream()) {
            List<String[]> filasExcel = excelPort.leer(in);
            for (int i = 1; i < filasExcel.size(); i++) {
                String[] row = filasExcel.get(i);
                if (filaVacia(row)) {
                    continue;
                }
                total++;
                int numeroFila = i + 1;
                try {
                    EmpleadoRequest request = mapRow(row);
                    validar(request);
                    boolean existe = empleadoRepository.findByCodigoEmpleado(request.codigoEmpleado()).isPresent();
                    if (existe) {
                        Integer id = empleadoRepository.findByCodigoEmpleado(request.codigoEmpleado()).orElseThrow().getIdEmpleado();
                        empleadoService.actualizar(id, request);
                        registrarDetalle(carga, numeroFila, ResultadoFilaCarga.ACTUALIZADO, "Actualizado", filas);
                    } else {
                        empleadoService.crear(request);
                        registrarDetalle(carga, numeroFila, ResultadoFilaCarga.INSERTADO, "Registrado", filas);
                    }
                    ok++;
                } catch (Exception ex) {
                    fail++;
                    log.warn("Error en fila {} de {}: {}", numeroFila, file.getOriginalFilename(), ex.getMessage());
                    registrarDetalle(carga, numeroFila, ResultadoFilaCarga.ERROR, ex.getMessage(), filas);
                }
            }
        } catch (Exception ex) {
            carga.setEstado(EstadoCarga.FALLIDA);
            carga.setMensaje(ex.getMessage());
            carga.setFechaFin(OffsetDateTime.now());
            log.error("No se pudo procesar la carga Excel idCarga={}", carga.getIdCarga(), ex);
            throw DomainException.badRequest("No se pudo leer el Excel: " + ex.getMessage());
        }
        carga.setTotalFilas(total);
        carga.setFilasExitosas(ok);
        carga.setFilasFallidas(fail);
        carga.setFechaFin(OffsetDateTime.now());
        carga.setEstado(fail == 0 ? EstadoCarga.COMPLETADA : EstadoCarga.COMPLETADA_CON_ERRORES);
        log.info("Carga Excel finalizada idCarga={} total={} ok={} fail={}", carga.getIdCarga(), total, ok, fail);
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

    private EmpleadoRequest mapRow(String[] row) {
        String areaNombre = cell(row, 11);
        String cargoNombre = cell(row, 12);
        String horarioNombre = cell(row, 13);
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
                cell(row, 0).toUpperCase(),
                enumOVacio(cell(row, 1), TipoDocumento.class, "tipo_documento"),
                cell(row, 2).toUpperCase().replaceAll("[^A-Z0-9]", ""),
                cell(row, 3),
                cell(row, 4),
                cell(row, 5),
                parseDate(cell(row, 6)),
                enumOVacio(cell(row, 7), SexoEmpleado.class, "sexo"),
                cell(row, 8).toLowerCase(),
                null,
                cell(row, 9).replaceAll("\\D", ""),
                null,
                parseDate(cell(row, 10)),
                null,
                idArea, idCargo, idHorario,
                enumOVacio(cell(row, 14), TipoContrato.class, "tipo_contrato"),
                enumOVacio(cell(row, 15), EstadoEmpleado.class, "estado"),
                null
        );
    }

    /** Celda vacía: el servicio aplica el valor por defecto del catálogo. */
    private <E extends Enum<E>> E enumOVacio(String value, Class<E> clase, String columna) {
        if (Strings.isNullOrEmpty(value) || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(clase, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw DomainException.badRequest("Valor no válido en la columna " + columna + ": " + value);
        }
    }

    private void validar(EmpleadoRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<EmpleadoRequest> first = violations.iterator().next();
            throw DomainException.badRequest(first.getMessage());
        }
    }

    private static boolean filaVacia(String[] row) {
        return Arrays.stream(row).allMatch(Strings::isNullOrEmpty);
    }

    private static String cell(String[] row, int idx) {
        if (row == null || idx >= row.length) {
            return "";
        }
        return Strings.nullToEmpty(row[idx]).trim();
    }

    private LocalDate parseDate(String value) {
        if (Strings.isNullOrEmpty(value) || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
