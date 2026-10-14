package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.model.Persona;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta que devuelve la API para una encomienda.
 *
 * Incluye el codigo de verificacion, que el personal necesita para reponerselo
 * al cliente que perdio su comprobante, pero nunca la clave de seguridad: esa
 * se guarda cifrada y no puede recuperarse.
 */
public record EnvioResponse(String codigoRastreo,
                            String codigoVerificacion,
                            EstadoEnvio estadoActual,
                            PersonaDto remitente,
                            PersonaDto destinatario,
                            List<PersonaDto> autorizados,
                            CargaDto carga,
                            TarifaDto tarifa,
                            String agenciaOrigenId,
                            String agenciaDestinoId,
                            List<EventoDto> historial,
                            EntregaDto entrega,
                            LocalDateTime fechaRegistro) {

    public static EnvioResponse from(Envio envio) {
        return new EnvioResponse(
                envio.getCodigoRastreo(),
                envio.getCodigoVerificacion(),
                envio.getEstadoActual(),
                persona(envio.getRemitente()),
                persona(envio.getDestinatario()),
                envio.getAutorizados() == null ? List.of()
                        : envio.getAutorizados().stream().map(EnvioResponse::persona).toList(),
                new CargaDto(envio.getCarga().getDescripcion(),
                        envio.getCarga().getPesoKg(),
                        envio.getCarga().getLargoCm(),
                        envio.getCarga().getAnchoCm(),
                        envio.getCarga().getAltoCm(),
                        envio.getCarga().isFragil()),
                TarifaDto.from(envio.getTarifa()),
                envio.getAgenciaOrigenId(),
                envio.getAgenciaDestinoId(),
                envio.getHistorial() == null ? List.of()
                        : envio.getHistorial().stream().map(EventoDto::from).toList(),
                EntregaDto.from(envio.getEntrega()),
                envio.getFechaRegistro());
    }

    private static PersonaDto persona(Persona p) {
        return p == null ? null : new PersonaDto(p.getNombres(), p.getDni(), p.getTelefono());
    }
}
