package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Rol;
import com.utp.roggerexpress.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        String nombreCompleto,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @NotBlank(message = "La agencia es obligatoria")
        String agenciaId) {

    public Usuario toEntity() {
        return Usuario.builder()
                .username(username)
                .password(password)
                .nombreCompleto(nombreCompleto)
                .rol(rol)
                .agenciaId(agenciaId)
                .activo(true)
                .build();
    }
}
