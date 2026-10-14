package com.utp.roggerexpress.security;

/**
 * Delimita que informacion puede ver quien consulta.
 *
 * La administracion ve toda la red; el resto del personal solo las encomiendas
 * de su propia agencia, sea como origen o como destino.
 */
public record AlcanceConsulta(boolean todaLaRed, String agenciaId) {

    public static AlcanceConsulta global() {
        return new AlcanceConsulta(true, null);
    }

    public static AlcanceConsulta deAgencia(String agenciaId) {
        return new AlcanceConsulta(false, agenciaId);
    }
}
