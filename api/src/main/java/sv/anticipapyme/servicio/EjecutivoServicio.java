package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.EjecutivoRespuesta;
import sv.anticipapyme.entidad.Ejecutivo;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.EjecutivoRepositorio;

import java.util.List;

@Service
public class EjecutivoServicio {

    private final EjecutivoRepositorio ejecutivos;

    public EjecutivoServicio(EjecutivoRepositorio ejecutivos) {
        this.ejecutivos = ejecutivos;
    }

    @Transactional(readOnly = true)
    public List<EjecutivoRespuesta> listar() {
        return ejecutivos.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EjecutivoRespuesta buscarPorId(Long id) {
        Ejecutivo ejecutivo = ejecutivos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejecutivo", id));
        return armarRespuesta(ejecutivo);
    }

    private EjecutivoRespuesta armarRespuesta(Ejecutivo e) {
        return new EjecutivoRespuesta(
                e.getId(), e.getCodigoEmpleado(), e.getNombres(), e.getApellidos(),
                e.getUsuario().getUsername(), e.getActivo());
    }
}
