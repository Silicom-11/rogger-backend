package com.utp.roggerexpress.exception;

/**
 * Se lanza cuando se intenta admitir una encomienda identica a otra registrada
 * hace muy poco: normalmente un segundo clic del operador o un reintento de red.
 */
public class RegistroDuplicadoException extends RuntimeException {

    private final String codigoExistente;

    public RegistroDuplicadoException(String codigoExistente, int minutos) {
        super("Hace menos de " + minutos + " minutos se registro una encomienda identica ("
                + codigoExistente + "). Si realmente son dos envios distintos, modifique algun dato.");
        this.codigoExistente = codigoExistente;
    }

    public String getCodigoExistente() {
        return codigoExistente;
    }
}
