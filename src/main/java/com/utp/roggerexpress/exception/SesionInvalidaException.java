package com.utp.roggerexpress.exception;

/**
 * El token de refresco no existe, ya fue usado, fue revocado o caduco.
 */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
