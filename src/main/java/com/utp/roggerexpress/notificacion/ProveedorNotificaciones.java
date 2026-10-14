package com.utp.roggerexpress.notificacion;

/**
 * Puerto de salida hacia el servicio que entrega los mensajes al cliente.
 *
 * El sistema no depende de un proveedor concreto: se elige por configuracion
 * (app.notificaciones.proveedor) y puede cambiarse de SMS a WhatsApp sin tocar
 * la logica de negocio.
 */
public interface ProveedorNotificaciones {

    /** Canal que atiende este proveedor: SMS, WHATSAPP o REGISTRO. */
    String canal();

    /**
     * true cuando el mensaje sale realmente hacia el telefono del cliente.
     * El proveedor de bitacora devuelve false para no dar por avisado a quien
     * no recibio nada.
     */
    boolean entregaReal();

    ResultadoNotificacion enviar(String destino, String mensaje);
}
