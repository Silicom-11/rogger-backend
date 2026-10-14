package com.utp.roggerexpress.exception;

import com.utp.roggerexpress.model.EstadoEnvio;

/** Se lanza cuando se intenta un cambio de estado no permitido. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoEnvio actual, EstadoEnvio solicitado) {
        super("No se puede pasar del estado " + actual + " al estado " + solicitado);
    }

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
