package sv.anticipapyme.excepcion;

public class FacturaNoConfirmadaException extends OperacionRechazadaException {
    public FacturaNoConfirmadaException(String codigoGeneracion, String estadoActual) {
        super(String.format("La factura %s tiene estado %s, no esta CONFIRMADA", codigoGeneracion, estadoActual));
    }
}