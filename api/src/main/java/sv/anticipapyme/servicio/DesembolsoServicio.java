package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.dto.DesembolsoRespuesta;
import sv.anticipapyme.entidad.Desembolso;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import sv.anticipapyme.repositorio.DesembolsoRepositorio;

import java.util.List;

@Service
public class DesembolsoServicio {

    private final DesembolsoRepositorio desembolsos;

    public DesembolsoServicio(DesembolsoRepositorio desembolsos) {
        this.desembolsos = desembolsos;
    }

    @Transactional(readOnly = true)
    public List<DesembolsoRespuesta> listar() {
        return desembolsos.findAll()
                .stream()
                .map(this::armarRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesembolsoRespuesta buscarPorId(Long id) {
        Desembolso desembolso = desembolsos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Desembolso", id));
        return armarRespuesta(desembolso);
    }

    private DesembolsoRespuesta armarRespuesta(Desembolso d) {
        return new DesembolsoRespuesta(
                d.getId(), d.getNumeroComprobante(),
                d.getOperacionFactoraje().getNumeroOperacion(),
                d.getCuentaDestino(), d.getFechaHora());
    }
}
