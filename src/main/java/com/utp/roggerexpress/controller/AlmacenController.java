package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.*;
import com.utp.roggerexpress.model.Entrega;
import com.utp.roggerexpress.security.UsuarioActual;
import com.utp.roggerexpress.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Modulo de almacen y entregas: control del ciclo de vida de la encomienda.
 *
 * El personal de una agencia solo puede mover las encomiendas que salen de ella
 * o llegan a ella; sobre las demas el sistema responde como si no existieran.
 */
@RestController
@RequestMapping("/api/v1/envios")
@Tag(name = "Almacen", description = "Control de estados y entrega de encomiendas")
public class AlmacenController {

    private final EnvioService envioService;
    private final UsuarioActual usuarioActual;

    public AlmacenController(EnvioService envioService, UsuarioActual usuarioActual) {
        this.envioService = envioService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/para-entrega")
    @Operation(summary = "Encomiendas que puede retirar quien presenta este documento")
    public ResponseEntity<List<EnvioResponse>> paraEntrega(@RequestParam String dni) {
        return ResponseEntity.ok(envioService.buscarParaEntrega(dni, usuarioActual.alcance())
                .stream().map(EnvioResponse::from).toList());
    }

    @PatchMapping("/{codigo}/estado")
    @Operation(summary = "Actualiza el estado logistico validando la transicion")
    public ResponseEntity<EnvioResponse> actualizarEstado(
            @PathVariable String codigo,
            @Valid @RequestBody CambioEstadoRequest request,
            @AuthenticationPrincipal UserDetails autenticado) {

        return ResponseEntity.ok(EnvioResponse.from(envioService.actualizarEstado(
                codigo, request.estado(), agencia(request.agenciaId()),
                usuario(request.usuario(), autenticado), request.observacion(),
                usuarioActual.alcance())));
    }

    @PatchMapping("/estado-lote")
    @Operation(summary = "Aplica el mismo cambio de estado a varias encomiendas")
    public ResponseEntity<ResultadoLoteResponse> actualizarEstadoLote(
            @Valid @RequestBody CambioEstadoLoteRequest request,
            @AuthenticationPrincipal UserDetails autenticado) {

        return ResponseEntity.ok(envioService.actualizarEstadoLote(
                request.codigos(), request.estado(), agencia(request.agenciaId()),
                usuario(null, autenticado), request.observacion(), usuarioActual.alcance()));
    }

    @PostMapping("/{codigo}/entrega")
    @Operation(summary = "Registra la entrega verificando el documento y la clave de seguridad")
    public ResponseEntity<EnvioResponse> registrarEntrega(
            @PathVariable String codigo,
            @Valid @RequestBody EntregaRequest request,
            @AuthenticationPrincipal UserDetails autenticado) {

        Entrega entrega = Entrega.builder()
                .receptorNombres(request.receptorNombres())
                .receptorDni(request.receptorDni())
                .parentesco(request.parentesco())
                .motivoSinClave(request.omitirClave() ? request.motivoSinClave() : null)
                .usuario(usuario(request.usuario(), autenticado))
                .build();

        String clave = request.omitirClave() ? null : request.claveRecojo();

        return ResponseEntity.ok(EnvioResponse.from(
                envioService.registrarEntrega(codigo, entrega, clave, usuarioActual.alcance())));
    }

    private String usuario(String indicado, UserDetails autenticado) {
        if (indicado != null && !indicado.isBlank()) {
            return indicado;
        }
        return autenticado == null ? null : autenticado.getUsername();
    }

    /**
     * La agencia que queda en el historial es siempre la del usuario que
     * ejecuta la accion; el dato enviado por el cliente solo se usa cuando
     * quien opera es la administracion, que no pertenece a una sede fija.
     */
    private String agencia(String indicada) {
        return usuarioActual.usuario()
                .filter(u -> u.getAgenciaId() != null && !usuarioActual.esAdministrador())
                .map(u -> u.getAgenciaId())
                .orElse(indicada);
    }
}
