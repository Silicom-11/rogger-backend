package com.utp.roggerexpress.dto;

import java.util.List;

/** Detalle de lo que ocurrio con cada encomienda de un cambio en lote. */
public record ResultadoLoteResponse(int total,
                                    int actualizados,
                                    List<String> codigosActualizados,
                                    List<Fallido> fallidos) {

    public record Fallido(String codigo, String motivo) {
    }
}
