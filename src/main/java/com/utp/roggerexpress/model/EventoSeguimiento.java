package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Documento embebido: cada cambio de estado registrado en el historial del envio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoSeguimiento {

    private EstadoEnvio estado;
    private LocalDateTime fecha;
    private String agenciaId;
    private String usuario;
    private String observacion;
}
