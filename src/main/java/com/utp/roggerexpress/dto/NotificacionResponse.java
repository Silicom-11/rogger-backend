package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.EstadoNotificacion;
import com.utp.roggerexpress.model.Notificacion;

import java.time.LocalDateTime;

/** Aviso al cliente tal como se muestra en la bitacora de mensajeria. */
public record NotificacionResponse(String id,
                                   String codigoRastreo,
                                   String canal,
                                   String destino,
                                   String mensaje,
                                   EstadoNotificacion estado,
                                   int intentos,
                                   boolean entregaReal,
                                   String ultimoError,
                                   String referenciaProveedor,
                                   String agenciaDestinoId,
                                   LocalDateTime fechaEnvio,
                                   LocalDateTime fechaActualizacion) {

    public static NotificacionResponse from(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getCodigoRastreo(), n.getCanal(),
                n.getDestino(), n.getMensaje(), n.getEstado(), n.getIntentos(),
                n.isEntregaReal(), n.getUltimoError(), n.getReferenciaProveedor(),
                n.getAgenciaDestinoId(),
                n.getFechaEnvio(), n.getFechaActualizacion());
    }
}
