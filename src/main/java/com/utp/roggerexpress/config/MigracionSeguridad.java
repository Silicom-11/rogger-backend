package com.utp.roggerexpress.config;

import com.utp.roggerexpress.model.Usuario;
import com.utp.roggerexpress.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migracion de seguridad idempotente.
 *
 * Versiones anteriores del sistema guardaban la contrasena en texto plano. Al
 * arrancar se detecta cualquier usuario cuya contrasena no sea un hash BCrypt
 * y se la reemplaza por el hash de la contrasena por defecto, dejando constancia
 * en el log para que el administrador la cambie.
 *
 * Se ejecuta antes de la carga inicial y no elimina ningun dato.
 */
@Component
@Order(1)
public class MigracionSeguridad implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionSeguridad.class);

    /** Contrasena por defecto asignada a los usuarios migrados. */
    private static final String CLAVE_POR_DEFECTO = "Rogger2026";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public MigracionSeguridad(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        List<Usuario> sinCifrar = usuarioRepository.findAll().stream()
                .filter(u -> u.getPassword() == null || !u.getPassword().startsWith("$2"))
                .toList();

        if (sinCifrar.isEmpty()) {
            return;
        }

        sinCifrar.forEach(usuario -> usuario.setPassword(passwordEncoder.encode(CLAVE_POR_DEFECTO)));
        usuarioRepository.saveAll(sinCifrar);

        log.warn("Migracion de seguridad: {} usuario(s) tenian la contrasena sin cifrar. "
                + "Se les asigno la contrasena por defecto '{}'. Cambiela desde el modulo de usuarios.",
                sinCifrar.size(), CLAVE_POR_DEFECTO);
    }
}
