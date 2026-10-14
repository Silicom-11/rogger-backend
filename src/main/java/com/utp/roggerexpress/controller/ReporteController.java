package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.ResumenResponse;
import com.utp.roggerexpress.security.UsuarioActual;
import com.utp.roggerexpress.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Modulo de reportes: indicadores del tablero de control.
 *
 * Los importes de toda la red son informacion economica de la empresa y se
 * devuelven unicamente a la administracion. El arqueo del propio usuario, en
 * cambio, se entrega siempre: quien atiende ventanilla necesita saber cuanto
 * cobro en su turno.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Indicadores de la operacion")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioActual usuarioActual;

    public ReporteController(ReporteService reporteService, UsuarioActual usuarioActual) {
        this.reporteService = reporteService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Indicadores para el tablero, acotados al alcance del usuario")
    public ResponseEntity<ResumenResponse> resumen() {
        return ResponseEntity.ok(reporteService.resumen(
                usuarioActual.alcance(), usuarioActual.esAdministrador(), usuarioActual.username()));
    }

    @GetMapping("/por-estado")
    @Operation(summary = "Cantidad de encomiendas agrupadas por estado")
    public ResponseEntity<Map<String, Long>> porEstado() {
        return ResponseEntity.ok(reporteService.conteoPorEstado(usuarioActual.alcance()));
    }
}
