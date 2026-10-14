package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.AnulacionRequest;
import com.utp.roggerexpress.dto.ClaveRecojoResponse;
import com.utp.roggerexpress.dto.ComprobanteResponse;
import com.utp.roggerexpress.dto.EnvioRequest;
import com.utp.roggerexpress.dto.EnvioResponse;
import com.utp.roggerexpress.dto.PaginaResponse;
import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.security.UsuarioActual;
import com.utp.roggerexpress.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo de admision: registro, busqueda, consulta y anulacion de encomiendas.
 *
 * Todas las consultas se resuelven dentro del alcance del usuario: el personal
 * de agencia solo ve las encomiendas de su sede.
 */
@RestController
@RequestMapping("/api/v1/envios")
@Tag(name = "Envios", description = "Admision, busqueda y consulta de encomiendas")
public class EnvioController {

    /** Maximo de registros que puede devolver una sola pagina. */
    private static final int TAMANIO_MAXIMO = 100;

    private final EnvioService envioService;
    private final UsuarioActual usuarioActual;

    public EnvioController(EnvioService envioService, UsuarioActual usuarioActual) {
        this.envioService = envioService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    @Operation(summary = "Registra una encomienda y devuelve el comprobante con sus claves")
    public ResponseEntity<ComprobanteResponse> registrar(@Valid @RequestBody EnvioRequest request,
                                                         @AuthenticationPrincipal UserDetails usuario) {

        String responsable = usuario == null ? null : usuario.getUsername();

        EnvioService.Admision admision =
                envioService.registrar(request.toEntity(), request.claveRecojo(), responsable);

        ComprobanteResponse comprobante = ComprobanteResponse.de(
                EnvioResponse.from(admision.envio()),
                admision.codigoVerificacion(),
                admision.claveRecojo());

        return ResponseEntity
                .created(URI.create("/api/v1/envios/" + comprobante.envio().codigoRastreo()))
                .body(comprobante);
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtiene el detalle completo de una encomienda de su agencia")
    public ResponseEntity<EnvioResponse> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(EnvioResponse.from(
                envioService.buscarVisible(codigo, usuarioActual.alcance())));
    }

    @GetMapping
    @Operation(summary = "Busqueda paginada con filtros por estado, agencia, fecha y texto")
    public ResponseEntity<PaginaResponse<EnvioResponse>> buscar(
            @RequestParam(required = false) EstadoEnvio estado,
            @RequestParam(required = false) String agenciaDestinoId,
            @RequestParam(required = false) String agenciaOrigenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {

        // Se acota el tamano de pagina para que una peticion no pueda pedir
        // toda la coleccion de una sola vez.
        int pag = Math.max(0, pagina);
        int tam = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO);

        var alcance = usuarioActual.alcance();

        List<EnvioResponse> contenido = envioService
                .buscar(estado, agenciaDestinoId, agenciaOrigenId, desde, hasta, texto, alcance, pag, tam)
                .stream().map(EnvioResponse::from).toList();

        long total = envioService.contar(estado, agenciaDestinoId, agenciaOrigenId,
                desde, hasta, texto, alcance);

        return ResponseEntity.ok(PaginaResponse.de(contenido, pag, tam, total));
    }

    @GetMapping("/{codigo}/siguientes-estados")
    @Operation(summary = "Estados a los que puede avanzar la encomienda")
    public ResponseEntity<List<EstadoEnvio>> siguientesEstados(@PathVariable String codigo) {
        return ResponseEntity.ok(EnvioService.siguientesEstados(
                envioService.buscarVisible(codigo, usuarioActual.alcance()).getEstadoActual()));
    }

    @PostMapping("/{codigo}/anulacion")
    @Operation(summary = "Anula una admision registrada por error, indicando el motivo")
    public ResponseEntity<EnvioResponse> anular(@PathVariable String codigo,
                                                @Valid @RequestBody AnulacionRequest request,
                                                @AuthenticationPrincipal UserDetails usuario) {

        String responsable = usuario == null ? null : usuario.getUsername();
        return ResponseEntity.ok(EnvioResponse.from(
                envioService.anular(codigo, request.motivo(), responsable, usuarioActual.alcance())));
    }

    @PostMapping("/{codigo}/clave-recojo")
    @Operation(summary = "Genera una clave de seguridad nueva cuando el cliente perdio la suya")
    public ResponseEntity<ClaveRecojoResponse> regenerarClave(@PathVariable String codigo,
                                                              @AuthenticationPrincipal UserDetails usuario) {

        String responsable = usuario == null ? null : usuario.getUsername();
        String clave = envioService.regenerarClaveRecojo(codigo, responsable, usuarioActual.alcance());

        return ResponseEntity.ok(ClaveRecojoResponse.de(codigo, clave));
    }
}
