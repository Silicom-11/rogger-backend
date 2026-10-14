package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Documento embebido: descripcion fisica de la carga que se transporta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carga {

    private String descripcion;
    private BigDecimal pesoKg;
    private Integer largoCm;
    private Integer anchoCm;
    private Integer altoCm;
    private boolean fragil;
}
