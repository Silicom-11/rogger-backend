package com.utp.roggerexpress.model;

/**
 * Las cuatro etapas que ve el cliente cuando rastrea su encomienda.
 *
 * El sistema maneja internamente nueve estados, pero al cliente no le sirve
 * distinguir entre "registrado" y "en almacen de origen": lo que necesita saber
 * es si su encomienda ya salio, si llego y si puede recogerla. Este enum traduce
 * el estado interno al lenguaje del cliente.
 */
public enum EtapaSeguimiento {

    EN_ORIGEN(1, "En origen", "Su encomienda fue recibida y está en la agencia de origen."),
    EN_TRANSITO(2, "En tránsito", "Su encomienda va en camino a la agencia de destino."),
    EN_DESTINO(3, "En destino", "Su encomienda llegó a la agencia de destino."),
    ENTREGADO(4, "Entregado", "Su encomienda fue entregada."),
    DETENIDO(0, "Requiere atención", "Su encomienda necesita una gestión en la agencia.");

    private final int numero;
    private final String etiqueta;
    private final String mensaje;

    EtapaSeguimiento(int numero, String etiqueta, String mensaje) {
        this.numero = numero;
        this.etiqueta = etiqueta;
        this.mensaje = mensaje;
    }

    public int getNumero() {
        return numero;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getMensaje() {
        return mensaje;
    }

    /** Traduce el estado interno de la encomienda a la etapa que ve el cliente. */
    public static EtapaSeguimiento de(EstadoEnvio estado) {
        if (estado == null) {
            return DETENIDO;
        }
        return switch (estado) {
            case REGISTRADO, EN_ALMACEN_ORIGEN -> EN_ORIGEN;
            case EN_TRANSITO -> EN_TRANSITO;
            case EN_ALMACEN_DESTINO, LISTO_PARA_RECOJO -> EN_DESTINO;
            case ENTREGADO -> ENTREGADO;
            case INCIDENCIA, DEVUELTO, ANULADO -> DETENIDO;
        };
    }

    /** Mensaje afinado para los estados que el cliente necesita entender mejor. */
    public static String mensajeDe(EstadoEnvio estado) {
        if (estado == EstadoEnvio.LISTO_PARA_RECOJO) {
            return "Su encomienda está lista para el recojo. Acérquese con su DNI y su clave de seguridad.";
        }
        if (estado == EstadoEnvio.DEVUELTO) {
            return "Su encomienda fue devuelta al remitente.";
        }
        if (estado == EstadoEnvio.ANULADO) {
            return "Esta admisión fue anulada en la agencia de origen.";
        }
        if (estado == EstadoEnvio.INCIDENCIA) {
            return "Su encomienda presenta una incidencia. Comuníquese con la agencia.";
        }
        return de(estado).getMensaje();
    }
}
