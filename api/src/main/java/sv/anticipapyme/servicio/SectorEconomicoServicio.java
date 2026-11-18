package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.SectorEconomicoRespuesta;
import sv.anticipapyme.entidad.SectorEconomico;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.SectorEconomicoRepositorio;

import java.util.List;

@Service
public class SectorEconomicoServicio {

    private final SectorEconomicoRepositorio sectores;

    public SectorEconomicoServicio(SectorEconomicoRepositorio sectores) {
        this.sectores = sectores;
    }

    @Transactional(readOnly = true)
    public List<SectorEconomicoRespuesta> listar() {
        return sectores.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectorEconomicoRespuesta buscarPorId(Long id) {
        SectorEconomico sector = sectores.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sector economico", id));
        return armarRespuesta(sector);
    }

    private SectorEconomicoRespuesta armarRespuesta(SectorEconomico s) {
        return new SectorEconomicoRespuesta(s.getId(), s.getCodigo(), s.getNombre(), s.getActivo());
    }
}
