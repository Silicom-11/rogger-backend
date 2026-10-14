package com.utp.roggerexpress.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.roggerexpress.security.FiltroLimiteSolicitudes;
import com.utp.roggerexpress.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuracion de seguridad de la API.
 *
 * La API es stateless: cada peticion se autentica con el token JWT enviado en
 * la cabecera Authorization. Las rutas publicas son el inicio de sesion, la
 * renovacion del acceso, el rastreo del cliente, el cotizador y la
 * documentacion; todas ellas pasan primero por el limitador de solicitudes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String origenesPermitidos;
    private final int limitePorMinuto;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${app.cors.origenes}") String origenesPermitidos,
                          @Value("${app.seguridad.limite-por-minuto:60}") int limitePorMinuto) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.origenesPermitidos = origenesPermitidos;
        this.limitePorMinuto = limitePorMinuto;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Sobre HTTPS se instruye al navegador a no volver a usar HTTP.
            .headers(h -> h
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(Duration.ofDays(180).toSeconds()))
                .frameOptions(frame -> frame.sameOrigin()))

            .authorizeHttpRequests(auth -> auth

                // El despacho interno de errores no vuelve a pasar por la
                // autenticacion: de lo contrario un error 500 se devolveria
                // como 401 y ocultaria la causa real.
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                // ---- rutas publicas ----
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/logout").permitAll()
                .requestMatchers("/api/v1/tracking/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/tarifas/cotizar").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // ---- cualquier usuario autenticado puede cambiar su clave ----
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/cambiar-clave").authenticated()

                // ---- admision: operador de ventanilla ----
                .requestMatchers(HttpMethod.POST, "/api/v1/envios").hasAnyRole("OPERADOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/envios/*/anulacion").hasAnyRole("OPERADOR", "ADMIN")

                // ---- almacen: mostrador, control de estados y entrega ----
                .requestMatchers(HttpMethod.GET, "/api/v1/envios/para-entrega").hasAnyRole("ALMACEN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/envios/estado-lote").hasAnyRole("ALMACEN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/envios/*/estado").hasAnyRole("ALMACEN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/envios/*/entrega").hasAnyRole("ALMACEN", "ADMIN")

                // ---- clave de seguridad: quien atiende al cliente ----
                .requestMatchers(HttpMethod.POST, "/api/v1/envios/*/clave-recojo")
                        .hasAnyRole("OPERADOR", "ALMACEN", "ADMIN")

                // ---- avisos al cliente: reenvio solo para quien opera el almacen ----
                .requestMatchers(HttpMethod.POST, "/api/v1/notificaciones/*/reintento")
                        .hasAnyRole("ALMACEN", "ADMIN")

                // ---- administracion ----
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/tarifario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/tarifario/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/agencias").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/agencias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/agencias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/agencias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tarifario/**").hasRole("ADMIN")

                // ---- el resto requiere sesion iniciada ----
                .anyRequest().authenticated())

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    escribirError(res, mapper, 401, "Unauthorized",
                            "Debe iniciar sesion para acceder a este recurso"))
                .accessDeniedHandler((req, res, e) ->
                    escribirError(res, mapper, 403, "Forbidden",
                            "Su rol no tiene permiso para realizar esta operacion")))

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // El limitador va delante de la autenticacion: un flujo masivo se
            // corta antes de gastar recursos verificando credenciales.
            .addFilterBefore(new FiltroLimiteSolicitudes(mapper, limitePorMinuto),
                    JwtAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse res, ObjectMapper mapper,
                               int estado, String error, String mensaje) throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", estado,
                "error", error,
                "mensaje", mensaje));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
