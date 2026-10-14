package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Entrega;
import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.model.EtapaSeguimiento;
import com.utp.roggerexpress.model.EventoSeguimiento;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta publica del rastreo.
 *
 * Muestra el recorrido en las cuatro etapas que le importan al cliente y oculta
 * lo que no necesita ver: el documento aparece enmascarado y no se expone el
 * telefono ni el detalle economico.
 */
public record TrackingResponse(String codigoRastreo,
                               int etapa,
                               String etapaEtiqueta,
                               String mensaje,
                               LocalDateTime desde,
                               boolean listoParaRecojo,
                               boolean finalizado,
                               String destinatario,
                               String documentoDestinatario,
                               String descripcionCarga,
                               String agenciaOrigen,
                               String agenciaDestino,
                               EntregaPublica entrega,
                               List<EventoDto> historial) {

    /** Constancia que se muestra al cliente cuando la encomienda ya fue entregada. */
    public record EntregaPublica(String receptorNombres,
                                 String documento,
                                 LocalDateTime fecha) {
    }

    public static TrackingResponse from(Envio envio, String nombreOrigen, String nombreDestino) {

        EstadoEnvio estado = envio.getEstadoActual();
        EtapaSeguimiento etapa = EtapaSeguimiento.de(estado);

        return new TrackingResponse(
                envio.getCodigoRastreo(),
                etapa.getNumero(),
                etapa.getEtiqueta(),
                EtapaSeguimiento.mensajeDe(estado),
                fechaDelEstado(envio),
                estado == EstadoEnvio.LISTO_PARA_RECOJO,
                estado == EstadoEnvio.ENTREGADO,
                envio.getDestinatario().getNombres(),
                enmascarar(envio.getDestinatario().getDni()),
                envio.getCarga().getDescripcion(),
                nombreOrigen,
                nombreDestino,
                entregaPublica(envio.getEntrega()),
                envio.getHistorial() == null ? List.of()
                        : envio.getHistorial().stream().map(EventoDto::from).toList());
    }

    /** Momento en que la encomienda entro en su estado actual. */
    private static LocalDateTime fechaDelEstado(Envio envio) {
        if (envio.getHistorial() == null || envio.getHistorial().isEmpty()) {
            return envio.getFechaRegistro();
        }
        EventoSeguimiento ultimo = envio.getHistorial().get(envio.getHistorial().size() - 1);
        return ultimo.getFecha();
    }

    private static EntregaPublica entregaPublica(Entrega e) {
        return e == null ? null
                : new EntregaPublica(e.getReceptorNombres(), enmascarar(e.getReceptorDni()), e.getFechaEntrega());
    }

    /** Deja visibles solo los ultimos digitos del documento. */
    private static String enmascarar(String documento) {
        if (documento == null || documento.length() < 4) {
            return "****";
        }
        return "*".repeat(documento.length() - 3) + documento.substring(documento.length() - 3);
    }
}
