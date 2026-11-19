package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.EstadoEnvio;

import java.math.BigDecimal;

/**
 * Resumen de una encomienda, para las consultas que proyectan campos.
 *
 * Existe porque devolver {@link EnvioResponse} a partir de un documento
 * proyectado seria enganoso: los campos que la consulta no trajo se publicarian
 * con su valor por omision, de modo que una lista de carga fragil mostraria
 * "fragil": false. Este tipo declara exactamente los campos que la proyeccion
 * pide, y asi la respuesta no afirma nada que no se haya leido.
 */
public record EnvioResumenResponse(String codigoRastreo,
                                   EstadoEnvio estadoActual,
                                   String destinatario,
                                   String descripcionCarga,
                                   BigDecimal pesoKg,
                                   boolean fragil,
                                   String agenciaDestinoId) {

    public static EnvioResumenResponse from(Envio envio) {
        return new EnvioResumenResponse(
                envio.getCodigoRastreo(),
                envio.getEstadoActual(),
                envio.getDestinatario() == null ? null : envio.getDestinatario().getNombres(),
                envio.getCarga() == null ? null : envio.getCarga().getDescripcion(),
                envio.getCarga() == null ? null : envio.getCarga().getPesoKg(),
                envio.getCarga() != null && envio.getCarga().isFragil(),
                envio.getAgenciaDestinoId());
    }
}
