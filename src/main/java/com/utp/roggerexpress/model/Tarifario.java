package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tarifario vigente por ruta. Reemplaza el criterio manual del operador.
 */
@Document(collection = "tarifas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarifario {

    @Id
    private String id;

    private String ruta;
    private BigDecimal montoBase;
    private BigDecimal precioPorKg;
    private Integer factorVolumetrico;
    private BigDecimal recargoFragil;
    private LocalDate vigenteDesde;

    @Builder.Default
    private boolean activo = true;
}
