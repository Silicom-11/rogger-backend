package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.Persona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de la peticion POST /api/v1/envios.
 */
public record EnvioRequest(

        @Valid @jakarta.validation.constraints.NotNull(message = "El remitente es obligatorio")
        PersonaDto remitente,

        @Valid @jakarta.validation.constraints.NotNull(message = "El destinatario es obligatorio")
        PersonaDto destinatario,

        /**
         * Hasta dos personas mas que pueden recoger la encomienda, ademas del
         * destinatario. Es el caso corriente de quien no puede acercarse.
         */
        @Valid
        @Size(max = 2, message = "Puede registrar como máximo dos personas autorizadas")
        List<PersonaDto> autorizados,

        @Valid @jakarta.validation.constraints.NotNull(message = "Los datos de la carga son obligatorios")
        CargaDto carga,

        /**
         * Clave de seguridad que se pedira al momento del recojo. Si el cliente
         * no dicta una, el sistema la genera.
         */
        @Pattern(regexp = "^$|^[0-9]{4,8}$", message = "La clave de seguridad debe tener entre 4 y 8 dígitos")
        String claveRecojo,

        @NotBlank(message = "La agencia de origen es obligatoria")
        String agenciaOrigenId,

        @NotBlank(message = "La agencia de destino es obligatoria")
        String agenciaDestinoId) {

    public Envio toEntity() {
        return Envio.builder()
                .remitente(new Persona(remitente.nombres(), remitente.dni(), remitente.telefono()))
                .destinatario(new Persona(destinatario.nombres(), destinatario.dni(), destinatario.telefono()))
                .autorizados(autorizados == null ? List.of()
                        : autorizados.stream()
                                .map(a -> new Persona(a.nombres(), a.dni(), a.telefono()))
                                .toList())
                .carga(carga.toEntity())
                .agenciaOrigenId(agenciaOrigenId)
                .agenciaDestinoId(agenciaDestinoId)
                .build();
    }
}
