package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.out.AfpPort;
import pe.andina.rrhh.application.port.out.RegimenLaboralPort;
import pe.andina.rrhh.application.port.out.ReglasPlanillaPort;
import pe.andina.rrhh.application.port.out.TramoRentaQuintaPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Afp;
import pe.andina.rrhh.domain.model.RegimenLaboral;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;

import java.util.List;

@Service
public class ReglasPlanillaService implements ReglasPlanillaPort {

    private final AfpPort afpRepository;
    private final RegimenLaboralPort regimenRepository;
    private final TramoRentaQuintaPort tramoRepository;

    public ReglasPlanillaService(AfpPort afpRepository, RegimenLaboralPort regimenRepository,
                                 TramoRentaQuintaPort tramoRepository) {
        this.afpRepository = afpRepository;
        this.regimenRepository = regimenRepository;
        this.tramoRepository = tramoRepository;
    }

    @Override
    public Afp afp(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw DomainException.badRequest("El contrato en AFP debe indicar la AFP");
        }
        return afpRepository.findById(codigo)
                .orElseThrow(() -> DomainException.badRequest("La AFP «" + codigo + "» no está registrada en Maestros › AFP"));
    }

    @Override
    public RegimenLaboral regimen(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return regimenRepository.findFirstByPorDefectoTrueAndActivoTrue()
                    .orElseThrow(() -> DomainException.badRequest("Configure un régimen laboral por defecto en Maestros › Regímenes laborales"));
        }
        return regimenRepository.findById(codigo)
                .orElseThrow(() -> DomainException.badRequest("El régimen laboral «" + codigo + "» no está registrado"));
    }

    @Override
    public List<TramoRentaQuinta> tramosQuinta() {
        List<TramoRentaQuinta> tramos = tramoRepository.findAllByOrderByOrdenAsc().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .toList();
        if (tramos.isEmpty()) {
            throw DomainException.badRequest("Configure los tramos de renta de 5ta categoría en Maestros");
        }
        return tramos;
    }
}
