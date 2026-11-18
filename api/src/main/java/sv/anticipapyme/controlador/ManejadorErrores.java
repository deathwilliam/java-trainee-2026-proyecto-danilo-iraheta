package sv.anticipapyme.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import sv.anticipapyme.excepcion.CredencialesInvalidasException;
import sv.anticipapyme.excepcion.DependenciasActivasException;
import sv.anticipapyme.excepcion.OperacionRechazadaException;
import sv.anticipapyme.excepcion.RecursoNoEncontradoException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        log.error("Error inesperado: {}", e.getMessage(), e);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado. Intente de nuevo o contacte a soporte.");
        problema.setTitle("Error interno");
        return problema;
    }

    @ExceptionHandler(OperacionRechazadaException.class)
    public ProblemDetail rechazada(OperacionRechazadaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Operacion rechazada");
        return problema;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Datos no validos");
        return problema;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("Recurso no encontrado");
        return problema;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflicto(DataIntegrityViolationException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion choca con datos que ya existen");
        problema.setTitle("Conflicto con los datos");
        return problema;
    }

    @ExceptionHandler(DependenciasActivasException.class)
    public ProblemDetail dependenciasActivas(DependenciasActivasException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("No se puede eliminar");
        return problema;
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problema.setTitle("Credenciales invalidas");
        return problema;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail noAutenticado(AuthenticationException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Falta un token valido o es invalido");
        problema.setTitle("No autenticado");
        return problema;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail sinPermiso(AccessDeniedException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tiene permiso para esta operacion");
        problema.setTitle("Sin permiso");
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Hay datos que no cumplen las validaciones");
        problema.setTitle("Datos no validos");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo no es un JSON valido, o tiene un valor que no se puede convertir");
        problema.setTitle("Cuerpo ilegible");
        return ResponseEntity.badRequest().body(problema);
    }
}