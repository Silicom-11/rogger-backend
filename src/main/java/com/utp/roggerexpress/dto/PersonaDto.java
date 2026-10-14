package com.utp.roggerexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos del remitente o del destinatario recibidos desde el formulario de ventanilla.
 */
public record PersonaDto(

        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 digitos")
        String dni,

        @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
        String telefono) {
}
