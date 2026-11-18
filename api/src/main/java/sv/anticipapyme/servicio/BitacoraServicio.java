package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.entidad.BitacoraOperacion;
import sv.anticipapyme.entidad.OperacionFactoraje;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.entidad.enums.ResultadoOperacion;
import sv.anticipapyme.entidad.enums.TipoOperacion;
import sv.anticipapyme.repositorio.BitacoraOperacionRepositorio;

@Service
public class BitacoraServicio {

    private final BitacoraOperacionRepositorio bitacoras;

    public BitacoraServicio(BitacoraOperacionRepositorio bitacoras) {
        this.bitacoras = bitacoras;
    }

    @Transactional
    public void registrarExitoso(Usuario usuario, OperacionFactoraje operacion, TipoOperacion tipo, String detalle) {
        bitacoras.save(new BitacoraOperacion(usuario, operacion, tipo, ResultadoOperacion.EXITOSO, detalle));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallido(Usuario usuario, TipoOperacion tipo, String detalle) {
        bitacoras.save(new BitacoraOperacion(usuario, null, tipo, ResultadoOperacion.FALLIDO, recortar(detalle)));
    }

    // detalle es varchar(255): un mensaje largo rompe el INSERT
    private String recortar(String detalle) {
        if (detalle == null) {
            return "Sin detalle";
        }
        return detalle.length() > 255 ? detalle.substring(0, 255) : detalle;
    }
}