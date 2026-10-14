package com.utp.roggerexpress.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Indicadores del tablero de control.
 *
 * Los importes de la red (ingresosHoy, ingresosMes, ticketPromedio) solo se
 * calculan para la administracion. En cambio misAdmisionesHoy y miCajaHoy son
 * el arqueo del propio usuario: quien atiende ventanilla necesita saber cuanto
 * cobro en su turno para cuadrar caja al cierre.
 */
public record ResumenResponse(long totalEnvios,
                              long enTransito,
                              long listosParaRecojo,
                              long entregadosHoy,
                              long porRecogerHace3Dias,
                              Long misAdmisionesHoy,
                              BigDecimal miCajaHoy,
                              BigDecimal ingresosHoy,
                              BigDecimal ingresosMes,
                              BigDecimal ticketPromedio,
                              Map<String, Long> porEstado,
                              List<ConteoAgencia> porAgenciaDestino) {

    public record ConteoAgencia(String agenciaId, String nombre, long cantidad) {
    }
}
