package com.utp.roggerexpress.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.roggerexpress.model.*;
import com.utp.roggerexpress.security.JwtAuthenticationFilter;
import com.utp.roggerexpress.security.UsuarioActual;
import com.utp.roggerexpress.service.EnvioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de la capa web: verifica el contrato HTTP del endpoint de registro
 * sin levantar la base de datos ni la cadena de seguridad.
 */
@WebMvcTest(controllers = EnvioController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnvioService envioService;

    // El controlador resuelve el alcance de datos del usuario; en esta
    // prueba de la capa web se sustituye por un doble.
    @MockBean
    private UsuarioActual usuarioActual;

    @Test
    @DisplayName("PI-01: POST /api/v1/envios devuelve 201, el codigo de rastreo y las claves del comprobante")
    void registrarDevuelve201() throws Exception {

        Envio guardado = Envio.builder()
                .codigoRastreo("RE-2026-000148")
                .codigoVerificacion("K7QM")
                .estadoActual(EstadoEnvio.REGISTRADO)
                .remitente(new Persona("Jorge Ramos", "45871236", "987654321"))
                .destinatario(new Persona("Luisa Quispe", "70125489", "964112233"))
                .carga(Carga.builder().descripcion("Caja de repuestos").pesoKg(new BigDecimal("12.5"))
                        .largoCm(40).anchoCm(30).altoCm(25).build())
                .tarifa(Tarifa.builder().pesoFacturable(new BigDecimal("12.50"))
                        .total(new BigDecimal("45.50")).moneda("PEN").build())
                .agenciaOrigenId("AG-LIM-VIC")
                .agenciaDestinoId("AG-HYO-CEN")
                .fechaRegistro(LocalDateTime.now())
                .build();

        when(envioService.registrar(any(Envio.class), nullable(String.class), nullable(String.class)))
                .thenReturn(new EnvioService.Admision(guardado, "K7QM", "482190"));

        Map<String, Object> cuerpo = Map.of(
                "remitente", Map.of("nombres", "Jorge Ramos", "dni", "45871236", "telefono", "987654321"),
                "destinatario", Map.of("nombres", "Luisa Quispe", "dni", "70125489", "telefono", "964112233"),
                "carga", Map.of("descripcion", "Caja de repuestos", "pesoKg", 12.5,
                        "largoCm", 40, "anchoCm", 30, "altoCm", 25, "fragil", false),
                "agenciaOrigenId", "AG-LIM-VIC",
                "agenciaDestinoId", "AG-HYO-CEN");

        mockMvc.perform(post("/api/v1/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.envio.codigoRastreo").value("RE-2026-000148"))
                .andExpect(jsonPath("$.envio.estadoActual").value("REGISTRADO"))
                .andExpect(jsonPath("$.codigoVerificacion").value("K7QM"))
                .andExpect(jsonPath("$.claveRecojo").value("482190"));
    }

    @Test
    @DisplayName("PI-02: POST /api/v1/envios con datos invalidos devuelve 400 y detalla los campos")
    void registrarConDatosInvalidosDevuelve400() throws Exception {

        Map<String, Object> cuerpo = Map.of(
                "remitente", Map.of("nombres", "", "dni", "123"),
                "destinatario", Map.of("nombres", "Luisa Quispe", "dni", "70125489"),
                "carga", Map.of("descripcion", "", "pesoKg", 0),
                "agenciaOrigenId", "",
                "agenciaDestinoId", "AG-HYO-CEN");

        mockMvc.perform(post("/api/v1/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").exists());
    }
}
