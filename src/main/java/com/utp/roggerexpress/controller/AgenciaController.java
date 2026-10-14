package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.AgenciaRequest;
import com.utp.roggerexpress.model.Agencia;
import com.utp.roggerexpress.service.AgenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Catalogo de agencias: CRUD completo sobre la coleccion agencias.
 */
@RestController
@RequestMapping("/api/v1/agencias")
@Tag(name = "Agencias", description = "Catalogo de agencias de la red")
public class AgenciaController {

    private final AgenciaService agenciaService;

    public AgenciaController(AgenciaService agenciaService) {
        this.agenciaService = agenciaService;
    }

    @GetMapping
    @Operation(summary = "Lista las agencias activas de la red")
    public ResponseEntity<List<Agencia>> listar() {
        return ResponseEntity.ok(agenciaService.listarActivas());
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtiene una agencia por su codigo")
    public ResponseEntity<Agencia> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(agenciaService.buscarPorCodigo(codigo));
    }

    @PostMapping
    @Operation(summary = "Registra una agencia nueva")
    public ResponseEntity<Agencia> registrar(@Valid @RequestBody AgenciaRequest request) {
        Agencia creada = agenciaService.registrar(request.toEntity());
        return ResponseEntity.created(URI.create("/api/v1/agencias/" + creada.getCodigo())).body(creada);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Actualiza los datos de una agencia")
    public ResponseEntity<Agencia> actualizar(@PathVariable String codigo,
                                              @Valid @RequestBody AgenciaRequest request) {
        return ResponseEntity.ok(agenciaService.actualizar(codigo, request.toEntity()));
    }

    @PatchMapping("/{codigo}/estado")
    @Operation(summary = "Activa o desactiva una agencia sin perder su historial")
    public ResponseEntity<Agencia> cambiarEstado(@PathVariable String codigo,
                                                 @RequestParam boolean activo) {
        return ResponseEntity.ok(agenciaService.cambiarEstado(codigo, activo));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Elimina una agencia que nunca movio carga")
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        agenciaService.eliminar(codigo);
        return ResponseEntity.noContent().build();
    }
}
