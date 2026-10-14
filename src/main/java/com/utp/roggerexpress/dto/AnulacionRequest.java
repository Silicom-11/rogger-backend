package com.utp.roggerexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/v1/envios/{codigo}/anulacion */
public record AnulacionRequest(

        @NotBlank(message = "Debe indicar el motivo de la anulacion")
        @Size(min = 5, message = "El motivo debe ser descriptivo (minimo 5 caracteres)")
        String motivo) {
}
