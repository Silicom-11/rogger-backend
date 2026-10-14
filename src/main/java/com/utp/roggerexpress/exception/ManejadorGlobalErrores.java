package com.utp.roggerexpress.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del dominio a respuestas HTTP con una estructura
 * uniforme, de modo que el front-end siempre reciba el mismo formato de error.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> transicionInvalida(TransicionInvalidaException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EntregaNoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> entregaNoPermitida(EntregaNoPermitidaException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> duplicado(RegistroDuplicadoException ex) {
        Map<String, Object> cuerpo = base(HttpStatus.CONFLICT, ex.getMessage());
        cuerpo.put("codigoExistente", ex.getCodigoExistente());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(DatosInvalidosException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, Object> cuerpo = base(HttpStatus.BAD_REQUEST, "Los datos enviados no son validos");
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }
        cuerpo.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    /**
     * Falta un dato obligatorio en la consulta. Es un error de quien llama, no
     * del servidor: devolverlo como 500 haria pensar que el sistema se cayo.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> parametroFaltante(MissingServletRequestParameterException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "Falta el dato '" + ex.getParameterName() + "' en la consulta");
    }

    /** Un valor que no corresponde al tipo esperado (una fecha o un estado mal escrito). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El valor enviado en '" + ex.getName() + "' no es válido");
    }

    /**
     * Cuenta bloqueada por intentos fallidos: 429 con el tiempo de espera, para
     * que el cliente sepa cuando puede reintentar.
     */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<Map<String, Object>> demasiadosIntentos(DemasiadosIntentosException ex) {
        Map<String, Object> cuerpo = base(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        cuerpo.put("segundosRestantes", ex.getSegundosRestantes());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getSegundosRestantes()))
                .body(cuerpo);
    }

    /** Token de refresco vencido, revocado o desconocido: 401. */
    @ExceptionHandler(SesionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> sesionInvalida(SesionInvalidaException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Credenciales incorrectas: 401. Debe declararse antes del manejador
     * generico, de lo contrario un error de contrasena se reportaria como 500
     * y el usuario recibiria un mensaje equivocado.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credencialesInvalidas(BadCredentialsException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos");
    }

    /** Usuario desactivado por el administrador: 403. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> usuarioDesactivado(DisabledException ex) {
        return construir(HttpStatus.FORBIDDEN,
                "Su usuario esta desactivado. Comuniquese con el administrador");
    }

    /** Cualquier otro fallo de autenticacion: 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> autenticacion(AuthenticationException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "No fue posible autenticar la solicitud");
    }

    /**
     * Cualquier error no previsto se devuelve como 500 con la misma estructura,
     * sin exponer la traza interna al cliente. El detalle queda en el log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> errorNoControlado(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado al procesar la solicitud");
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(base(estado, mensaje));
    }

    private Map<String, Object> base(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
    }
}
