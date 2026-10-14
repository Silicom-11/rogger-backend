package com.utp.roggerexpress.service;

import com.utp.roggerexpress.dto.ClaveTemporalResponse;
import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.model.Usuario;
import com.utp.roggerexpress.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UsuarioService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    /** El usuario creado por un administrador debe definir su propia clave al ingresar. */
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new DatosInvalidosException("Ya existe un usuario con el nombre " + usuario.getUsername());
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setDebeCambiarClave(true);
        return usuarioRepository.save(usuario);
    }

    public Usuario cambiarEstado(String id, boolean activo) {
        Usuario usuario = buscar(id);
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    /**
     * Restablece la contrasena con una clave temporal que se muestra una sola
     * vez al administrador. El trabajador debera cambiarla al ingresar.
     */
    public ClaveTemporalResponse restablecerClave(String id) {

        Usuario usuario = buscar(id);
        String temporal = generarClaveTemporal();

        usuario.setPassword(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarClave(true);
        usuarioRepository.save(usuario);

        return new ClaveTemporalResponse(usuario.getUsername(), temporal,
                "Entregue esta clave al trabajador. Se le pedira cambiarla al ingresar.");
    }

    private Usuario buscar(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    private String generarClaveTemporal() {
        StringBuilder clave = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            clave.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return clave.toString();
    }
}
