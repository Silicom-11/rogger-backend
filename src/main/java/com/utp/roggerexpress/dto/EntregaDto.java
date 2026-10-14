package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Entrega;

import java.time.LocalDateTime;

/** Constancia de la entrega tal como la muestra la ficha de la encomienda. */
public record EntregaDto(String receptorNombres,
                         String receptorDni,
                         String parentesco,
                         String relacion,
                         boolean claveVerificada,
                         String motivoSinClave,
                         LocalDateTime fechaEntrega,
                         String usuario) {

    public static EntregaDto from(Entrega e) {
        return e == null ? null
                : new EntregaDto(e.getReceptorNombres(), e.getReceptorDni(), e.getParentesco(),
                        e.getRelacion(), e.isClaveVerificada(), e.getMotivoSinClave(),
                        e.getFechaEntrega(), e.getUsuario());
    }
}
