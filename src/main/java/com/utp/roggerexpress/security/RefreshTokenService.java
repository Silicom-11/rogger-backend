package com.utp.roggerexpress.security;

import com.utp.roggerexpress.model.SesionRefresh;
import com.utp.roggerexpress.repository.SesionRefreshRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Emision, validacion y rotacion de los tokens de refresco.
 *
 * El token viaja al cliente en claro una sola vez; en la base solo queda su
 * hash SHA-256. Al refrescar se emite uno nuevo y se revoca el anterior, de
 * modo que un token robado deja de servir apenas el usuario legitimo lo use.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SesionRefreshRepository repositorio;
    private final int diasVigencia;

    public RefreshTokenService(SesionRefreshRepository repositorio,
                               @Value("${app.jwt.refresh-dias:7}") int diasVigencia) {
        this.repositorio = repositorio;
        this.diasVigencia = diasVigencia;
    }

    /** Crea una sesion nueva y devuelve el token en claro. */
    public String emitir(String username) {

        byte[] bytes = new byte[48];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repositorio.save(SesionRefresh.builder()
                .tokenHash(hash(token))
                .username(username)
                .creadaEn(LocalDateTime.now())
                .expiraEn(LocalDateTime.now().plusDays(diasVigencia))
                .revocada(false)
                .build());

        return token;
    }

    /** Devuelve la sesion vigente asociada al token, si existe. */
    public Optional<SesionRefresh> validar(String token) {
        return repositorio.findByTokenHash(hash(token)).filter(SesionRefresh::estaVigente);
    }

    /** Revoca la sesion indicada (por rotacion o por cierre de sesion). */
    public void revocar(SesionRefresh sesion) {
        sesion.setRevocada(true);
        repositorio.save(sesion);
    }

    public void revocarPorToken(String token) {
        repositorio.findByTokenHash(hash(token)).ifPresent(this::revocar);
    }

    /** Cierra todas las sesiones del usuario (por ejemplo, al cambiar la clave). */
    public void revocarTodas(String username) {
        repositorio.findByUsernameAndRevocadaFalse(username).forEach(this::revocar);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta plataforma", ex);
        }
    }
}
