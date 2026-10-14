package com.utp.roggerexpress.notificacion;

/**
 * Respuesta del proveedor de mensajeria ante un intento de despacho.
 */
public record ResultadoNotificacion(boolean aceptado, String referencia, String error) {

    public static ResultadoNotificacion aceptado(String referencia) {
        return new ResultadoNotificacion(true, referencia, null);
    }

    public static ResultadoNotificacion rechazado(String error) {
        return new ResultadoNotificacion(false, null, error);
    }
}
