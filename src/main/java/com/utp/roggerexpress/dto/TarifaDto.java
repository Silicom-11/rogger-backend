package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Tarifa;

import java.math.BigDecimal;

public record TarifaDto(BigDecimal pesoVolumetrico,
                        BigDecimal pesoFacturable,
                        BigDecimal montoBase,
                        BigDecimal recargo,
                        BigDecimal total,
                        String moneda) {

    public static TarifaDto from(Tarifa tarifa) {
        if (tarifa == null) {
            return null;
        }
        return new TarifaDto(tarifa.getPesoVolumetrico(), tarifa.getPesoFacturable(),
                tarifa.getMontoBase(), tarifa.getRecargo(), tarifa.getTotal(), tarifa.getMoneda());
    }
}
