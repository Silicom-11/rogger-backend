package com.utp.roggerexpress.exception;

/** Se lanza cuando el recurso solicitado no existe en la base de datos. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
