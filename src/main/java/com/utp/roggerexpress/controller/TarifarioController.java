package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.TarifarioRequest;
import com.utp.roggerexpress.model.Tarifario;
import com.utp.roggerexpress.service.TarifarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tarifario por ruta: CRUD completo sobre la coleccion tarifas.
 */
@RestController
@RequestMapping("/api/v1/tarifario")
@Tag(name = "Tarifario", description = "Tarifas por ruta")
public class TarifarioController {

    private final TarifarioService tarifarioService;

    public TarifarioController(TarifarioService tarifarioService) {
        this.tarifarioService = tarifarioService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los tarifarios cargados")
    public ResponseEntity<List<Tarifario>> listar() {
        return ResponseEntity.ok(tarifarioService.listar());
    }

    @GetMapping("/vigentes")
    @Operation(summary = "Lista solo los tarifarios vigentes, ordenados por ruta")
    public ResponseEntity<List<Tarifario>> vigentes() {
        return ResponseEntity.ok(tarifarioService.listarVigentes());
    }

    @PostMapping
    @Operation(summary = "Carga un tarifario nuevo y desactiva el anterior de la misma ruta")
    public ResponseEntity<Tarifario> registrar(@Valid @RequestBody TarifarioRequest request) {
        return ResponseEntity.ok(tarifarioService.registrar(request.toEntity()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Corrige los importes de un tarifario ya cargado")
    public ResponseEntity<Tarifario> actualizar(@PathVariable String id,
                                                @Valid @RequestBody TarifarioRequest request) {
        return ResponseEntity.ok(tarifarioService.actualizar(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un tarifario cargado por error")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        tarifarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
