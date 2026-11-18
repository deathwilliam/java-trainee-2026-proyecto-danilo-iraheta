package sv.anticipapyme.excepcion;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String tipo, Long id) {
        super(String.format("No se encontro %s con id %d", tipo, id));
    }
}
