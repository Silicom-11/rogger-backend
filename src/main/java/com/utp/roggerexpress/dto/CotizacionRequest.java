package com.utp.roggerexpress.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la peticion POST /api/v1/tarifas/cotizar. Permite conocer el precio
 * antes de registrar la encomienda.
 */
public record CotizacionRequest(

        @Valid @NotNull(message = "Los datos de la carga son obligatorios")
        CargaDto carga,

        String ruta) {
}
