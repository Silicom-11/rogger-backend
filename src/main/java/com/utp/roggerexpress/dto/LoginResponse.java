package com.utp.roggerexpress.dto;

/**
 * Respuesta del inicio de sesion.
 *
 * El token de acceso es de vida corta; el de refresco permite renovarlo sin
 * volver a pedir la contrasena y puede revocarse desde el servidor.
 */
public record LoginResponse(String token,
                            String refreshToken,
                            String tipo,
                            long expiraEnSegundos,
                            UsuarioResponse usuario) {

    public static LoginResponse de(String token, String refreshToken,
                                   long segundos, UsuarioResponse usuario) {
        return new LoginResponse(token, refreshToken, "Bearer", segundos, usuario);
    }
}
