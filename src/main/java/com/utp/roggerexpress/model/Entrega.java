package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Documento embebido: evidencia de la entrega final de la encomienda.
 *
 * Guarda no solo a quien se entrego, sino como se verifico su derecho a
 * recibirla: si presento la clave de seguridad o si el personal la entrego
 * validando unicamente la identidad, y en ese caso por que motivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entrega {

    private String receptorNombres;
    private String receptorDni;
    private String parentesco;

    /** TITULAR cuando recoge el destinatario; AUTORIZADO cuando es un contacto registrado. */
    private String relacion;

    /** true cuando quien recogio digito correctamente la clave de seguridad. */
    private boolean claveVerificada;

    /** Motivo por el que se entrego sin la clave, cuando corresponde. */
    private String motivoSinClave;

    private LocalDateTime fechaEntrega;
    private String usuario;
}
