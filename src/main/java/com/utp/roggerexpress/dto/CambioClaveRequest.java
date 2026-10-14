package com.utp.roggerexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/v1/auth/cambiar-clave */
public record CambioClaveRequest(

        @NotBlank(message = "Debe ingresar su contrasena actual")
        String claveActual,

        @NotBlank(message = "Debe ingresar la nueva contrasena")
        @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
        String claveNueva) {
}
