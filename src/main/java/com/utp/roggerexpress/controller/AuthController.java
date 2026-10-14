package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.CambioClaveRequest;
import com.utp.roggerexpress.dto.LoginRequest;
import com.utp.roggerexpress.dto.LoginResponse;
import com.utp.roggerexpress.dto.RefreshRequest;
import com.utp.roggerexpress.dto.UsuarioResponse;
import com.utp.roggerexpress.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Modulo de acceso: inicio de sesion, renovacion, perfil y cambio de contrasena.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Inicio de sesion, renovacion y contrasena")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Valida las credenciales y devuelve el token de acceso y el de refresco")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.iniciarSesion(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el token de acceso a partir del token de refresco")
    public ResponseEntity<LoginResponse> refrescar(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refrescar(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesion invalidando el token de refresco")
    public ResponseEntity<Void> cerrarSesion(@Valid @RequestBody RefreshRequest request) {
        authService.cerrarSesion(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve los datos del usuario autenticado")
    public ResponseEntity<UsuarioResponse> perfil(@AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(authService.perfil(usuario.getUsername()));
    }

    @PostMapping("/cambiar-clave")
    @Operation(summary = "Cambia la contrasena del usuario autenticado y cierra sus otras sesiones")
    public ResponseEntity<UsuarioResponse> cambiarClave(
            @AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody CambioClaveRequest request) {

        return ResponseEntity.ok(authService.cambiarClave(usuario.getUsername(), request));
    }
}
