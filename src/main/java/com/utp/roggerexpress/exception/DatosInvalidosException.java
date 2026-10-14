package com.utp.roggerexpress.exception;

/** Se lanza cuando los datos recibidos no cumplen una regla de negocio. */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
