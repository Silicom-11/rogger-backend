package com.utp.roggerexpress.security;

import com.utp.roggerexpress.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Generacion y validacion de los tokens JWT que protegen la API.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    /** Genera el token con el usuario, su rol y su agencia. */
    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                // Identificador unico del token: dos tokens emitidos en el
                // mismo segundo deben poder distinguirse uno del otro.
                .id(UUID.randomUUID().toString())
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombreCompleto())
                .claim("agenciaId", usuario.getAgenciaId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public String extraerUsuario(String token) {
        return leer(token).getSubject();
    }

    public boolean esValido(String token) {
        try {
            return leer(token).getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long segundosDeVigencia() {
        return expiracionMinutos * 60;
    }

    private Claims leer(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
