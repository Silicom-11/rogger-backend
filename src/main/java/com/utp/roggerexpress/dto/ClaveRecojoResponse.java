package com.utp.roggerexpress.dto;

/** Nueva clave de seguridad generada para una encomienda ya admitida. */
public record ClaveRecojoResponse(String codigoRastreo,
                                  String claveRecojo,
                                  String aviso) {

    public static ClaveRecojoResponse de(String codigoRastreo, String clave) {
        return new ClaveRecojoResponse(codigoRastreo, clave,
                "La clave anterior dejó de ser válida. Comuníquesela al cliente: no vuelve a mostrarse.");
    }
}
