package com.utp.roggerexpress.security;

import com.utp.roggerexpress.exception.DemasiadosIntentosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena los ataques de fuerza bruta contra el inicio de sesion.
 *
 * Cuenta los intentos fallidos por usuario y bloquea temporalmente la cuenta
 * cuando se supera el limite. El contador se reinicia con un ingreso correcto.
 */
@Service
public class ControlIntentosService {

    private static final Logger log = LoggerFactory.getLogger(ControlIntentosService.class);

    private record Intentos(int fallidos, Instant bloqueadoHasta) {
    }

    private final Map<String, Intentos> registro = new ConcurrentHashMap<>();

    private final int maximoIntentos;
    private final Duration bloqueo;

    public ControlIntentosService(
            @Value("${app.seguridad.max-intentos:5}") int maximoIntentos,
            @Value("${app.seguridad.bloqueo-minutos:15}") int bloqueoMinutos) {
        this.maximoIntentos = maximoIntentos;
        this.bloqueo = Duration.ofMinutes(bloqueoMinutos);
    }

    /** Rechaza la peticion si la cuenta esta bloqueada en este momento. */
    public void verificar(String username) {

        Intentos actual = registro.get(clave(username));
        if (actual == null || actual.bloqueadoHasta() == null) {
            return;
        }

        long restantes = Duration.between(Instant.now(), actual.bloqueadoHasta()).getSeconds();
        if (restantes > 0) {
            throw new DemasiadosIntentosException(restantes);
        }

        registro.remove(clave(username));
    }

    /** Registra un intento fallido y bloquea la cuenta al llegar al limite. */
    public void registrarFallo(String username) {

        registro.compute(clave(username), (k, previo) -> {
            int fallidos = (previo == null ? 0 : previo.fallidos()) + 1;

            if (fallidos >= maximoIntentos) {
                log.warn("Cuenta '{}' bloqueada temporalmente tras {} intentos fallidos", username, fallidos);
                return new Intentos(fallidos, Instant.now().plus(bloqueo));
            }
            return new Intentos(fallidos, null);
        });
    }

    /** Un ingreso correcto limpia el historial de intentos. */
    public void registrarExito(String username) {
        registro.remove(clave(username));
    }

    public int intentosFallidos(String username) {
        Intentos actual = registro.get(clave(username));
        return actual == null ? 0 : actual.fallidos();
    }

    private String clave(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
