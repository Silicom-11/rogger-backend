package com.utp.roggerexpress.model;

/**
 * Estados por los que atraviesa una encomienda durante su ciclo de vida.
 */
public enum EstadoEnvio {

    REGISTRADO,
    EN_ALMACEN_ORIGEN,
    EN_TRANSITO,
    EN_ALMACEN_DESTINO,
    LISTO_PARA_RECOJO,
    ENTREGADO,
    INCIDENCIA,
    DEVUELTO,

    /** Admision anulada por error de registro; no continua el flujo logistico. */
    ANULADO
}
