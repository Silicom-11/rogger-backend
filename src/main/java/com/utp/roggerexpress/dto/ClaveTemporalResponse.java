package com.utp.roggerexpress.dto;

/**
 * Resultado del restablecimiento de contrasena: la clave temporal se entrega
 * una sola vez al administrador para que se la comunique al trabajador.
 */
public record ClaveTemporalResponse(String username,
                                    String claveTemporal,
                                    String mensaje) {
}
