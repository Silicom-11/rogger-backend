package com.utp.roggerexpress.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Proveedor por defecto: deja constancia del mensaje en la bitacora y en el log
 * del servidor, sin enviarlo a ninguna red.
 *
 * Es el que opera mientras la empresa no contrate una pasarela de SMS o
 * WhatsApp. Marca los avisos como entrega no real para que el personal sepa
 * que el cliente todavia no fue contactado.
 */
@Component
@ConditionalOnProperty(name = "app.notificaciones.proveedor", havingValue = "registro", matchIfMissing = true)
public class ProveedorRegistro implements ProveedorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ProveedorRegistro.class);

    @Override
    public String canal() {
        return "REGISTRO";
    }

    @Override
    public boolean entregaReal() {
        return false;
    }

    @Override
    public ResultadoNotificacion enviar(String destino, String mensaje) {
        log.info("[bitacora] aviso para {}: {}", destino, mensaje);
        return ResultadoNotificacion.aceptado("bitacora-local");
    }
}
