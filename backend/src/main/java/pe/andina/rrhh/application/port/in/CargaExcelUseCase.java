package pe.andina.rrhh.application.port.in;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.andina.rrhh.application.dto.AppDtos.CargaFilaResponse;
import pe.andina.rrhh.application.dto.AppDtos.CargaResponse;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.domain.model.CargaMasiva;
import pe.andina.rrhh.domain.model.CargaMasivaDetalle;
import pe.andina.rrhh.domain.model.enums.EstadoCarga;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.ResultadoFilaCarga;
import pe.andina.rrhh.domain.model.enums.TipoContrato;
import pe.andina.rrhh.domain.model.enums.TipoDocumento;

public interface CargaExcelUseCase {
    byte[] plantilla();
    CargaResponse cargar(MultipartFile file);
}
