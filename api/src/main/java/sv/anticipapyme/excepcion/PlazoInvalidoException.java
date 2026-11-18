package sv.anticipapyme.excepcion;

public class PlazoInvalidoException extends OperacionRechazadaException {
    public PlazoInvalidoException(String codigoGeneracion) {
        super(String.format("La factura %s vence en menos de 15 dias", codigoGeneracion));
    }

    public PlazoInvalidoException(String codigoGeneracion, int plazoMaximoDias) {
        super(String.format("La factura %s vence en mas de %d dias", codigoGeneracion, plazoMaximoDias));
    }
}