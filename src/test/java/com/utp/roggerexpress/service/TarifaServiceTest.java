package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.model.Carga;
import com.utp.roggerexpress.model.Tarifa;
import com.utp.roggerexpress.repository.TarifarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la regla de negocio del tarifario (TDD).
 * No requieren conexion a MongoDB: el repositorio se simula con Mockito.
 */
class TarifaServiceTest {

    private TarifarioRepository tarifarioRepository;
    private TarifaService tarifaService;

    @BeforeEach
    void setUp() {
        tarifarioRepository = mock(TarifarioRepository.class);
        when(tarifarioRepository.findFirstByRutaAndActivoTrueOrderByVigenteDesdeDesc(anyString()))
                .thenReturn(Optional.of(TarifaService.tarifarioPorDefecto()));
        tarifaService = new TarifaService(tarifarioRepository);
    }

    @Test
    @DisplayName("PU-01: cobra por el peso real cuando supera al peso volumetrico")
    void cobraPorPesoReal() {
        Carga carga = Carga.builder().descripcion("Caja de repuestos")
                .pesoKg(new BigDecimal("12.5")).largoCm(40).anchoCm(30).altoCm(25).build();

        Tarifa tarifa = tarifaService.calcular(carga, "Lima-Huancayo");

        assertEquals(new BigDecimal("5.00"), tarifa.getPesoVolumetrico());
        assertEquals(new BigDecimal("12.50"), tarifa.getPesoFacturable());
        assertEquals(new BigDecimal("45.50"), tarifa.getTotal());
    }

    @Test
    @DisplayName("PU-02: cobra por el peso volumetrico cuando supera al peso real")
    void cobraPorPesoVolumetrico() {
        Carga carga = Carga.builder().descripcion("Cajas de tecnopor")
                .pesoKg(new BigDecimal("2.0")).largoCm(60).anchoCm(50).altoCm(40).build();

        Tarifa tarifa = tarifaService.calcular(carga, "Lima-Huancayo");

        assertEquals(new BigDecimal("20.00"), tarifa.getPesoFacturable());
        assertEquals(new BigDecimal("68.00"), tarifa.getTotal());
    }

    @Test
    @DisplayName("PU-03: aplica el recargo cuando la carga es fragil")
    void aplicaRecargoFragil() {
        Carga carga = Carga.builder().descripcion("Vajilla")
                .pesoKg(new BigDecimal("12.5")).largoCm(40).anchoCm(30).altoCm(25)
                .fragil(true).build();

        Tarifa tarifa = tarifaService.calcular(carga, "Lima-Huancayo");

        assertEquals(new BigDecimal("5.00"), tarifa.getRecargo());
        assertEquals(new BigDecimal("50.50"), tarifa.getTotal());
    }

    @Test
    @DisplayName("PU-04: rechaza una carga con peso invalido")
    void rechazaPesoInvalido() {
        Carga carga = Carga.builder().descripcion("Sobre")
                .pesoKg(BigDecimal.ZERO).largoCm(20).anchoCm(15).altoCm(5).build();

        assertThrows(DatosInvalidosException.class,
                () -> tarifaService.calcular(carga, "Lima-Huancayo"));
    }

    @Test
    @DisplayName("PU-05: rechaza dimensiones invalidas")
    void rechazaDimensionesInvalidas() {
        Carga carga = Carga.builder().descripcion("Paquete")
                .pesoKg(new BigDecimal("3.0")).largoCm(0).anchoCm(15).altoCm(5).build();

        assertThrows(DatosInvalidosException.class,
                () -> tarifaService.calcular(carga, "Lima-Huancayo"));
    }
}
