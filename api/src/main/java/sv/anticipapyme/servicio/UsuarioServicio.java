package sv.anticipapyme.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.anticipapyme.entidad.Usuario;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.repositorio.UsuarioRepositorio;

@Service
public class UsuarioServicio {

    private final UsuarioRepositorio usuarios;

    public UsuarioServicio(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return usuarios.findByUsername(username)
                .orElseThrow(() -> new OperacionRechazadaException("No se pudo procesar la solicitud"));
    }
}