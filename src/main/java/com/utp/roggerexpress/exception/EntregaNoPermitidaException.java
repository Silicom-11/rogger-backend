package com.utp.roggerexpress.exception;

/** Se lanza cuando la entrega no cumple las validaciones de identidad o de estado. */
public class EntregaNoPermitidaException extends RuntimeException {

    public EntregaNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
