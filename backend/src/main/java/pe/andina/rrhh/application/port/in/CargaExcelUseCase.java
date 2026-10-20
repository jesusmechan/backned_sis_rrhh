package pe.andina.rrhh.application.port.in;

import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.application.dto.AppDtos.CargaResponse;

public interface CargaExcelUseCase {
    byte[] plantilla();
    CargaResponse cargar(MultipartFile file);
}
