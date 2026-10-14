package com.utp.roggerexpress.dto;

/**
 * Respuesta de la admision: la encomienda registrada junto con los dos datos
 * que solo se muestran en este momento.
 *
 * El codigo de verificacion permite al cliente consultar su encomienda y la
 * clave de seguridad se le pedira al recogerla. La clave no vuelve a mostrarse:
 * en la base solo queda su forma cifrada.
 */
public record ComprobanteResponse(EnvioResponse envio,
                                  String codigoVerificacion,
                                  String claveRecojo,
                                  String aviso) {

    public static ComprobanteResponse de(EnvioResponse envio, String codigoVerificacion, String claveRecojo) {
        return new ComprobanteResponse(envio, codigoVerificacion, claveRecojo,
                "Entregue al cliente el código de verificación y la clave de seguridad. "
                        + "La clave no vuelve a mostrarse: si se pierde, debe generarse una nueva.");
    }
}
