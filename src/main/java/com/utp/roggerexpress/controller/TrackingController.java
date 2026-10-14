package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.TrackingResponse;
import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.service.AgenciaService;
import com.utp.roggerexpress.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modulo de rastreo: consulta que utiliza el cliente final sin iniciar sesion.
 *
 * Se piden los dos datos que figuran en el comprobante, el numero de la
 * encomienda y su codigo de verificacion, del mismo modo que en el resto de las
 * empresas de encomiendas del pais.
 */
@RestController
@RequestMapping("/api/v1/tracking")
@Tag(name = "Tracking", description = "Consulta publica del estado de la encomienda")
public class TrackingController {

    private final EnvioService envioService;
    private final AgenciaService agenciaService;

    public TrackingController(EnvioService envioService, AgenciaService agenciaService) {
        this.envioService = envioService;
        this.agenciaService = agenciaService;
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consulta el estado de una encomienda con su codigo y su codigo de verificacion")
    public ResponseEntity<TrackingResponse> rastrear(@PathVariable String codigo,
                                                     @RequestParam String verificacion) {

        Envio envio = envioService.rastrear(codigo, verificacion);

        return ResponseEntity.ok(TrackingResponse.from(envio,
                nombreAgencia(envio.getAgenciaOrigenId()),
                nombreAgencia(envio.getAgenciaDestinoId())));
    }

    /** Al cliente se le muestra el nombre de la agencia, no su codigo interno. */
    private String nombreAgencia(String codigo) {
        try {
            return agenciaService.buscarPorCodigo(codigo).getNombre();
        } catch (RuntimeException ex) {
            return codigo;
        }
    }
}
