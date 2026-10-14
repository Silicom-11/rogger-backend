package com.utp.roggerexpress.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita cuantas peticiones por minuto puede hacer un mismo origen contra los
 * accesos publicos de la API (inicio de sesion, renovacion, rastreo y
 * cotizador).
 *
 * Son los unicos puntos que se pueden llamar sin haber iniciado sesion, y por
 * lo tanto los unicos expuestos a un flujo automatizado. Al superarse el limite
 * se responde 429 con la cabecera Retry-After.
 *
 * El conteo se hace en memoria y por instancia; para varios servidores detras
 * de un balanceador corresponde llevarlo a un almacen compartido.
 */
public class FiltroLimiteSolicitudes extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroLimiteSolicitudes.class);

    private static final long VENTANA_MS = 60_000L;
    private static final int MAXIMO_ORIGENES = 10_000;

    private record Ventana(long inicio, int contador) {
    }

    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final int limitePorMinuto;

    public FiltroLimiteSolicitudes(ObjectMapper mapper, int limitePorMinuto) {
        this.mapper = mapper;
        this.limitePorMinuto = limitePorMinuto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        boolean publica = ruta.startsWith("/api/v1/auth/")
                || ruta.startsWith("/api/v1/tracking/")
                || ruta.startsWith("/api/v1/tarifas/cotizar");
        return !publica;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String origen = origen(request);
        long ahora = System.currentTimeMillis();

        Ventana ventana = ventanas.compute(origen, (clave, previa) -> {
            if (previa == null || ahora - previa.inicio() >= VENTANA_MS) {
                return new Ventana(ahora, 1);
            }
            return new Ventana(previa.inicio(), previa.contador() + 1);
        });

        if (ventanas.size() > MAXIMO_ORIGENES) {
            ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicio() >= VENTANA_MS);
        }

        if (ventana.contador() > limitePorMinuto) {
            long esperaSegundos = Math.max(1, (VENTANA_MS - (ahora - ventana.inicio())) / 1000);
            log.warn("Limite de solicitudes superado por {} en {}", origen, request.getRequestURI());
            responderExcedido(response, esperaSegundos);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Direccion de origen segun la conexion. No se usa X-Forwarded-For porque
     * cualquiera podria enviarla y saltarse el limite; si el sistema se publica
     * detras de un proxy, corresponde configurar server.forward-headers-strategy.
     */
    private String origen(HttpServletRequest request) {
        String direccion = request.getRemoteAddr();
        return direccion == null ? "desconocido" : direccion;
    }

    private void responderExcedido(HttpServletResponse response, long esperaSegundos) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(esperaSegundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 429,
                "error", "Too Many Requests",
                "mensaje", "Demasiadas solicitudes. Espere " + esperaSegundos + " segundo(s) e intente de nuevo.",
                "segundosRestantes", esperaSegundos));
    }
}
