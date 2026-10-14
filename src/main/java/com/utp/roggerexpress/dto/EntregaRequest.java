package com.utp.roggerexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de la peticion POST /api/v1/envios/{codigo}/entrega.
 *
 * En el mostrador se piden dos cosas: el documento de quien recoge y la clave
 * de seguridad. Cuando el cliente no la recuerda, el personal puede entregar
 * validando solo la identidad, pero debe dejar constancia del motivo.
 */
public record EntregaRequest(

        @NotBlank(message = "El nombre del receptor es obligatorio")
        String receptorNombres,

        @NotBlank(message = "El DNI del receptor es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 digitos")
        String receptorDni,

        String parentesco,

        /** Clave de seguridad digitada por quien recoge. */
        String claveRecojo,

        /** true cuando se entrega validando solo la identidad. */
        boolean omitirClave,

        /** Obligatorio si se omite la clave: queda registrado en el historial. */
        String motivoSinClave,

        String usuario) {
}
