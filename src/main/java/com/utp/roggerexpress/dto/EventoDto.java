package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.model.EventoSeguimiento;

import java.time.LocalDateTime;

public record EventoDto(EstadoEnvio estado,
                        LocalDateTime fecha,
                        String agenciaId,
                        String usuario,
                        String observacion) {

    public static EventoDto from(EventoSeguimiento evento) {
        return new EventoDto(evento.getEstado(), evento.getFecha(),
                evento.getAgenciaId(), evento.getUsuario(), evento.getObservacion());
    }
}
