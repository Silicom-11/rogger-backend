package com.utp.roggerexpress.notificacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proveedor real: entrega el mensaje a una pasarela de SMS / WhatsApp por HTTP.
 *
 * Se activa con app.notificaciones.proveedor=http y necesita la URL y el token
 * de la cuenta contratada. Cualquier respuesta distinta de 2xx se devuelve como
 * rechazo, con el codigo que respondio la pasarela, para que el aviso quede en
 * ERROR y el reintento automatico lo tome.
 *
 * La respuesta se lee como texto y se interpreta despues: las pasarelas
 * comerciales no siempre declaran application/json y el aviso no debe darse por
 * fallido solo porque la cabecera venga distinta de la esperada.
 */
@Component
@ConditionalOnProperty(name = "app.notificaciones.proveedor", havingValue = "http")
public class ProveedorHttp implements ProveedorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ProveedorHttp.class);

    /** Nombres habituales del identificador del mensaje segun el proveedor. */
    private static final List<String> CLAVES_ID = List.of("id", "messageId", "message_id", "sid", "uuid");

    private final RestClient cliente;
    private final ObjectMapper mapper;
    private final String canal;
    private final String remitente;

    public ProveedorHttp(ObjectMapper mapper,
                         @Value("${app.notificaciones.url}") String url,
                         @Value("${app.notificaciones.token:}") String token,
                         @Value("${app.notificaciones.canal:SMS}") String canal,
                         @Value("${app.notificaciones.remitente:RoggerExp}") String remitente,
                         @Value("${app.notificaciones.timeout-segundos:10}") int timeout) {

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "El proveedor de notificaciones esta configurado como 'http' pero falta "
                            + "app.notificaciones.url con la direccion de la pasarela contratada");
        }

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) Duration.ofSeconds(timeout).toMillis());
        fabrica.setReadTimeout((int) Duration.ofSeconds(timeout).toMillis());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (token != null && !token.isBlank()) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.cliente = builder.build();
        this.mapper = mapper;
        this.canal = canal;
        this.remitente = remitente;
    }

    @Override
    public String canal() {
        return canal;
    }

    @Override
    public boolean entregaReal() {
        return true;
    }

    @Override
    public ResultadoNotificacion enviar(String destino, String mensaje) {

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("to", destino);
        cuerpo.put("from", remitente);
        cuerpo.put("channel", canal);
        cuerpo.put("message", mensaje);

        try {
            ResponseEntity<String> respuesta = cliente.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toEntity(String.class);

            return ResultadoNotificacion.aceptado(identificador(respuesta.getBody()));

        } catch (RestClientResponseException ex) {
            // La pasarela contesto, pero con un codigo de error.
            String detalle = ex.getStatusCode().value() + " " + ex.getStatusText();
            log.warn("La pasarela rechazo el aviso para {}: {}", destino, detalle);
            return ResultadoNotificacion.rechazado("La pasarela respondio " + detalle);

        } catch (ResourceAccessException ex) {
            // No hubo respuesta: pasarela caida, sin red o fuera de tiempo.
            log.warn("No se pudo contactar la pasarela para {}: {}", destino, ex.getMessage());
            return ResultadoNotificacion.rechazado("No se pudo contactar la pasarela de mensajeria");

        } catch (RuntimeException ex) {
            log.warn("Fallo inesperado al enviar el aviso para {}: {}", destino, ex.getMessage());
            return ResultadoNotificacion.rechazado(ex.getMessage());
        }
    }

    /** Busca el identificador del mensaje en la respuesta, sin exigir un formato fijo. */
    private String identificador(String cuerpo) {

        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin-referencia";
        }
        try {
            JsonNode raiz = mapper.readTree(cuerpo);
            for (String clave : CLAVES_ID) {
                JsonNode valor = raiz.get(clave);
                if (valor != null && !valor.isNull()) {
                    return valor.asText();
                }
            }
        } catch (Exception ex) {
            log.debug("La pasarela no devolvio un JSON interpretable: {}", ex.getMessage());
        }
        return "sin-referencia";
    }
}
