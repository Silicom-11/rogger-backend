package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.EstadoEnvio;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de PATCH /api/v1/envios/estado-lote: permite al personal de almacen
 * mover varias encomiendas de una sola vez (por ejemplo, al descargar la unidad).
 */
public record CambioEstadoLoteRequest(

        @NotEmpty(message = "Debe indicar al menos una encomienda")
        List<String> codigos,

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoEnvio estado,

        String agenciaId,
        String observacion) {
}
