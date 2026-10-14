package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Bitacora de los avisos enviados al cliente.
 *
 * Guarda el resultado de cada intento: que proveedor lo atendio, cuantas veces
 * se intento y por que fallo. Sin esta bitacora la empresa no podria demostrar
 * que aviso al destinatario ni recuperar los mensajes que no salieron.
 */
@Document(collection = "notificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    private String id;

    private String envioId;

    @Indexed
    private String codigoRastreo;

    /** Canal por el que se despacho: SMS, WHATSAPP o REGISTRO (bitacora local). */
    private String canal;

    private String destino;
    private String mensaje;

    @Indexed
    private EstadoNotificacion estado;

    /** Agencia de destino de la encomienda; delimita quien puede ver el aviso. */
    private String agenciaDestinoId;

    /** Numero de intentos de despacho realizados. */
    @Builder.Default
    private int intentos = 0;

    /** Motivo del ultimo fallo, cuando el estado es ERROR. */
    private String ultimoError;

    /** Identificador que devolvio el proveedor, util para auditoria. */
    private String referenciaProveedor;

    /**
     * false cuando el mensaje solo quedo registrado en la bitacora local
     * porque no hay proveedor de mensajeria configurado.
     */
    @Builder.Default
    private boolean entregaReal = false;

    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaActualizacion;
}
