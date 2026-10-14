package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.ClaveTemporalResponse;
import com.utp.roggerexpress.dto.UsuarioRequest;
import com.utp.roggerexpress.dto.UsuarioResponse;
import com.utp.roggerexpress.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Modulo de administracion: gestion del personal del sistema.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestion del personal y sus roles")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Lista el personal registrado")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar().stream().map(UsuarioResponse::from).toList());
    }

    @PostMapping
    @Operation(summary = "Registra un nuevo usuario con su rol")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.registrar(request.toEntity())));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa o desactiva un usuario")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable String id,
                                                         @RequestParam boolean activo) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.cambiarEstado(id, activo)));
    }

    @PostMapping("/{id}/reset-clave")
    @Operation(summary = "Restablece la contrasena y devuelve una clave temporal")
    public ResponseEntity<ClaveTemporalResponse> restablecerClave(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.restablecerClave(id));
    }
}
