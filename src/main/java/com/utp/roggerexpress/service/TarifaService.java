package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.model.Carga;
import com.utp.roggerexpress.model.Tarifa;
import com.utp.roggerexpress.model.Tarifario;
import com.utp.roggerexpress.repository.TarifarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Regla de negocio central del tarifario.
 *
 * La tarifa se calcula sobre el PESO FACTURABLE, que es el mayor entre el peso
 * real de la carga y su peso volumetrico. Con esto se elimina el criterio
 * discrecional del operador de turno (problema especifico PE-05).
 */
@Service
public class TarifaService {

    private static final int ESCALA = 2;
    private static final String MONEDA = "PEN";

    private final TarifarioRepository tarifarioRepository;

    public TarifaService(TarifarioRepository tarifarioRepository) {
        this.tarifarioRepository = tarifarioRepository;
    }

    /** Calcula la tarifa de una carga para la ruta indicada. */
    public Tarifa calcular(Carga carga, String ruta) {

        validar(carga);

        Tarifario tarifario = tarifarioRepository
                .findFirstByRutaAndActivoTrueOrderByVigenteDesdeDesc(ruta)
                .orElseGet(TarifaService::tarifarioPorDefecto);

        BigDecimal volumen = BigDecimal.valueOf(carga.getLargoCm())
                .multiply(BigDecimal.valueOf(carga.getAnchoCm()))
                .multiply(BigDecimal.valueOf(carga.getAltoCm()));

        BigDecimal pesoVolumetrico = volumen
                .divide(BigDecimal.valueOf(tarifario.getFactorVolumetrico()), ESCALA, RoundingMode.HALF_UP);

        BigDecimal pesoReal = carga.getPesoKg().setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal pesoFacturable = pesoReal.max(pesoVolumetrico);

        BigDecimal recargo = carga.isFragil()
                ? tarifario.getRecargoFragil().setScale(ESCALA, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(ESCALA);

        BigDecimal total = tarifario.getMontoBase()
                .add(pesoFacturable.multiply(tarifario.getPrecioPorKg()))
                .add(recargo)
                .setScale(ESCALA, RoundingMode.HALF_UP);

        return Tarifa.builder()
                .pesoVolumetrico(pesoVolumetrico)
                .pesoFacturable(pesoFacturable)
                .montoBase(tarifario.getMontoBase().setScale(ESCALA, RoundingMode.HALF_UP))
                .recargo(recargo)
                .total(total)
                .moneda(MONEDA)
                .build();
    }

    private void validar(Carga carga) {
        if (carga == null) {
            throw new DatosInvalidosException("Los datos de la carga son obligatorios");
        }
        if (carga.getPesoKg() == null || carga.getPesoKg().signum() <= 0) {
            throw new DatosInvalidosException("El peso de la carga debe ser mayor a cero");
        }
        if (carga.getLargoCm() == null || carga.getAnchoCm() == null || carga.getAltoCm() == null
                || carga.getLargoCm() <= 0 || carga.getAnchoCm() <= 0 || carga.getAltoCm() <= 0) {
            throw new DatosInvalidosException("Las dimensiones de la carga deben ser mayores a cero");
        }
    }

    /** Tarifario base de la ruta Lima - Huancayo usado cuando no hay uno configurado. */
    public static Tarifario tarifarioPorDefecto() {
        return Tarifario.builder()
                .ruta("Lima-Huancayo")
                .montoBase(new BigDecimal("8.00"))
                .precioPorKg(new BigDecimal("3.00"))
                .factorVolumetrico(6000)
                .recargoFragil(new BigDecimal("5.00"))
                .vigenteDesde(LocalDate.now())
                .activo(true)
                .build();
    }
}
