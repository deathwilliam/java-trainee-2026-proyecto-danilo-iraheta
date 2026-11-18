package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.OperacionRespuesta;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.EmpresaRepositorio;
import sv.anticipapyme.repositorio.OperacionFactorajeRepositorio;

import java.util.List;

@Service
public class OperacionServicio {

    private final OperacionFactorajeRepositorio operaciones;
    private final EmpresaRepositorio empresas;

    public OperacionServicio(OperacionFactorajeRepositorio operaciones, EmpresaRepositorio empresas) {
        this.operaciones = operaciones;
        this.empresas = empresas;
    }

    @Transactional(readOnly = true)
    public List<OperacionRespuesta> listar(String username, boolean verTodo) {
        List<OperacionFactoraje> encontradas = verTodo
                ? operaciones.findAll()
                : empresas.findByUsuarioUsername(username)
                .map(e -> operaciones.findByEmpresaCedenteId(e.getId()))
                .orElseGet(List::of);
        return encontradas.stream().map(this::armarRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public OperacionRespuesta buscarPorId(Long id, String username, boolean verTodo) {
        OperacionFactoraje operacion = operaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operacion de factoraje", id));
        if (!verTodo && !perteneceOperacion(operacion, username)) {
            throw new RecursoNoEncontradoException("Operacion de factoraje", id);
        }
        return armarRespuesta(operacion);
    }

    private boolean perteneceOperacion(OperacionFactoraje operacion, String username) {
        return empresas.findByUsuarioUsername(username)
                .map(e -> e.getId().equals(operacion.getEmpresaCedente().getId()))
                .orElse(false);
    }

    private OperacionRespuesta armarRespuesta(OperacionFactoraje o) {
        String ejecutivo = o.getEjecutivo().getNombres() + " " + o.getEjecutivo().getApellidos();
        return new OperacionRespuesta(
                o.getId(), o.getNumeroOperacion(), o.getEmpresaCedente().getNombreLegal(),
                o.getCupoPagador().getNumeroCupo(), ejecutivo,
                o.getTasaDescuentoAplicada(), o.getTasaMoraAplicada(),
                o.getMontoNominalTotal(), o.getMontoDescuentoTotal(),
                o.getEstado().name(), o.getFechaHoraOperacion());
    }
}
