package com.utp.roggerexpress.security;

import com.utp.roggerexpress.model.Rol;
import com.utp.roggerexpress.model.Usuario;
import com.utp.roggerexpress.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resuelve quien esta ejecutando la peticion y con que alcance de datos.
 */
@Component
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName());
    }

    public String username() {
        return usuario().map(Usuario::getUsername).orElse(null);
    }

    public boolean esAdministrador() {
        return usuario().map(u -> u.getRol() == Rol.ADMIN).orElse(false);
    }

    /**
     * Alcance de datos del usuario: la administracion ve toda la red, el resto
     * del personal solo su agencia.
     */
    public AlcanceConsulta alcance() {
        return usuario()
                .map(u -> u.getRol() == Rol.ADMIN
                        ? AlcanceConsulta.global()
                        : AlcanceConsulta.deAgencia(u.getAgenciaId()))
                .orElseGet(AlcanceConsulta::global);
    }
}
