package com.utp.roggerexpress.model;

/** Situacion de un aviso al cliente dentro de la bitacora de mensajeria. */
public enum EstadoNotificacion {

    /** Creada, todavia no entregada al proveedor. */
    PENDIENTE,

    /** El proveedor acepto el mensaje. */
    ENVIADO,

    /** El proveedor rechazo el mensaje o no respondio; admite reintento. */
    ERROR
}
