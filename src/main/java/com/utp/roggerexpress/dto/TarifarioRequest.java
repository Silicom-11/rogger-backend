package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Tarifario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TarifarioRequest(

        @NotBlank(message = "La ruta es obligatoria")
        String ruta,

        @NotNull @DecimalMin(value = "0.0", message = "El monto base no puede ser negativo")
        BigDecimal montoBase,

        @NotNull @DecimalMin(value = "0.1", message = "El precio por kilogramo debe ser mayor a cero")
        BigDecimal precioPorKg,

        @NotNull @Positive(message = "El factor volumetrico debe ser mayor a cero")
        Integer factorVolumetrico,

        @NotNull @DecimalMin(value = "0.0", message = "El recargo no puede ser negativo")
        BigDecimal recargoFragil) {

    public Tarifario toEntity() {
        return Tarifario.builder()
                .ruta(ruta)
                .montoBase(montoBase)
                .precioPorKg(precioPorKg)
                .factorVolumetrico(factorVolumetrico)
                .recargoFragil(recargoFragil)
                .vigenteDesde(LocalDate.now())
                .activo(true)
                .build();
    }
}
