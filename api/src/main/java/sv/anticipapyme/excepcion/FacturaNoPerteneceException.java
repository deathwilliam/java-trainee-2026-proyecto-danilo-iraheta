package sv.anticipapyme.excepcion;

public class FacturaNoPerteneceException extends OperacionRechazadaException {

    private FacturaNoPerteneceException(String mensaje) {
        super(mensaje);
    }

    public static FacturaNoPerteneceException deOtraCedente(String codigoGeneracion) {
        return new FacturaNoPerteneceException(
                String.format("La factura %s no pertenece a la empresa cedente", codigoGeneracion));
    }

    public static FacturaNoPerteneceException deOtraPagadora(String codigoGeneracion) {
        return new FacturaNoPerteneceException(
                String.format("La factura %s no la debe la pagadora del cupo", codigoGeneracion));
    }
}