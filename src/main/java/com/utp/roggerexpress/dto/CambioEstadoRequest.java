package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.EstadoEnvio;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la peticion PATCH /api/v1/envios/{codigo}/estado.
 */
public record CambioEstadoRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoEnvio estado,

        String agenciaId,
        String usuario,
        String observacion) {
}
