package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.EnvioResponse;
import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.repository.EnvioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas declaradas sobre la coleccion de encomiendas.
 *
 * Cada endpoint se apoya en una consulta escrita con @Query en el repositorio,
 * sobre los campos del documento y no sobre nombres fisicos de tablas. Es el
 * papel que cumple JPQL cuando la base es relacional.
 */
@RestController
@RequestMapping("/api/v1/consultas")
@Tag(name = "Consultas", description = "Consultas declaradas sobre las encomiendas")
public class ConsultaController {

    private final EnvioRepository envioRepository;

    public ConsultaController(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    @GetMapping("/fragiles")
    @Operation(summary = "Carga fragil todavia en circulacion, con proyeccion de campos")
    public ResponseEntity<List<EnvioResponse>> fragiles() {
        return ResponseEntity.ok(envioRepository.buscarFragilesPendientes()
                .stream().map(EnvioResponse::from).toList());
    }

    @GetMapping("/por-importe")
    @Operation(summary = "Encomiendas cuyo importe alcanza el monto indicado, de mayor a menor")
    public ResponseEntity<List<EnvioResponse>> porImporte(
            @RequestParam(defaultValue = "30") BigDecimal minimo) {

        return ResponseEntity.ok(envioRepository.buscarDesdeImporte(minimo)
                .stream().map(EnvioResponse::from).toList());
    }

    @GetMapping("/por-documento")
    @Operation(summary = "Todo lo que una persona envio o espera recibir")
    public ResponseEntity<List<EnvioResponse>> porDocumento(@RequestParam String dni) {
        return ResponseEntity.ok(envioRepository.buscarPorDocumento(dni)
                .stream().map(EnvioResponse::from).toList());
    }

    @GetMapping("/por-periodo")
    @Operation(summary = "Encomiendas de un estado admitidas dentro de un rango de fechas")
    public ResponseEntity<List<EnvioResponse>> porPeriodo(
            @RequestParam EstadoEnvio estado,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(envioRepository
                .buscarPorEstadoYPeriodo(estado, desde.atStartOfDay(), hasta.atTime(23, 59, 59))
                .stream().map(EnvioResponse::from).toList());
    }
}
