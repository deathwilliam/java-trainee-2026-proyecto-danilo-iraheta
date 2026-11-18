package sv.anticipapyme.excepcion;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Usuario o contrasena incorrectos");
    }
}