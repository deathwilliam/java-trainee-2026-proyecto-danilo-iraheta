package sv.anticipapyme.excepcion;

import java.math.BigDecimal;

public class CupoInsuficienteException extends OperacionRechazadaException {
    public CupoInsuficienteException(BigDecimal solicitado, BigDecimal disponible) {
        super(String.format("El monto de $%.2f supera el cupo disponible de $%.2f", solicitado, disponible));
    }
}