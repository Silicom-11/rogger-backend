package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

/**
 * Documento embebido: resultado del calculo de la tarifa del envio.
 *
 * Los importes se almacenan como Decimal128 para poder agregarlos (sumas y
 * promedios) directamente en las consultas de MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal pesoVolumetrico;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal pesoFacturable;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal montoBase;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal recargo;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal total;

    private String moneda;
}
