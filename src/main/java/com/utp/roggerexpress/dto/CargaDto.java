package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Carga;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Datos fisicos de la carga. Las validaciones evitan que lleguen pesos o
 * dimensiones invalidas a la capa de servicio.
 */
public record CargaDto(

        @NotBlank(message = "La descripcion de la carga es obligatoria")
        String descripcion,

        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "0.1", message = "El peso debe ser mayor a cero")
        @DecimalMax(value = "1000.0", message = "El peso no puede superar los 1000 kg")
        BigDecimal pesoKg,

        @NotNull(message = "El largo es obligatorio")
        @Positive(message = "El largo debe ser mayor a cero")
        @Max(value = 300, message = "El largo no puede superar los 300 cm")
        Integer largoCm,

        @NotNull(message = "El ancho es obligatorio")
        @Positive(message = "El ancho debe ser mayor a cero")
        @Max(value = 300, message = "El ancho no puede superar los 300 cm")
        Integer anchoCm,

        @NotNull(message = "El alto es obligatorio")
        @Positive(message = "El alto debe ser mayor a cero")
        @Max(value = 300, message = "El alto no puede superar los 300 cm")
        Integer altoCm,

        boolean fragil) {

    public Carga toEntity() {
        return Carga.builder()
                .descripcion(descripcion)
                .pesoKg(pesoKg)
                .largoCm(largoCm)
                .anchoCm(anchoCm)
                .altoCm(altoCm)
                .fragil(fragil)
                .build();
    }
}
