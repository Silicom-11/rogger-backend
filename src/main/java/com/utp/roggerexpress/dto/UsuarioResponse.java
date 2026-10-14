package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Rol;
import com.utp.roggerexpress.model.Usuario;

/** Datos del usuario que se devuelven al front-end (nunca la contrasena). */
public record UsuarioResponse(String id,
                              String username,
                              String nombreCompleto,
                              Rol rol,
                              String agenciaId,
                              boolean activo,
                              boolean debeCambiarClave) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(),
                usuario.getNombreCompleto(), usuario.getRol(),
                usuario.getAgenciaId(), usuario.isActivo(), usuario.isDebeCambiarClave());
    }
}
