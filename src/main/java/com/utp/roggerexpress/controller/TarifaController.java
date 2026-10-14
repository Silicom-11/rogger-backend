package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.CotizacionRequest;
import com.utp.roggerexpress.dto.TarifaDto;
import com.utp.roggerexpress.service.TarifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modulo de tarifario: cotizacion previa al registro de la encomienda.
 */
@RestController
@RequestMapping("/api/v1/tarifas")
@Tag(name = "Tarifas", description = "Cotizacion automatica del costo de envio")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @PostMapping("/cotizar")
    @Operation(summary = "Calcula la tarifa segun el peso facturable, sin registrar el envio")
    public ResponseEntity<TarifaDto> cotizar(@Valid @RequestBody CotizacionRequest request) {

        String ruta = request.ruta() == null ? "Lima-Huancayo" : request.ruta();

        return ResponseEntity.ok(TarifaDto.from(
                tarifaService.calcular(request.carga().toEntity(), ruta)));
    }
}
