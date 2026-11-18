package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.CategoriaRiesgoRespuesta;
import sv.anticipapyme.entidad.CategoriaRiesgo;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.CategoriaRiesgoRepositorio;

import java.util.List;

@Service
public class CategoriaRiesgoServicio {

    private final CategoriaRiesgoRepositorio categorias;

    public CategoriaRiesgoServicio(CategoriaRiesgoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriaRiesgoRespuesta> listar() {
        return categorias.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaRiesgoRespuesta buscarPorId(Long id) {
        CategoriaRiesgo categoria = categorias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria de riesgo", id));
        return armarRespuesta(categoria);
    }

    private CategoriaRiesgoRespuesta armarRespuesta(CategoriaRiesgo c) {
        return new CategoriaRiesgoRespuesta(
                c.getId(), c.getCodigo(), c.getDescripcion(),
                c.getTasaDescuentoAnual(), c.getTasaMoraAnual(), c.getPlazoMaximoDias(), c.getActivo());
    }
}
