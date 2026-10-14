package com.utp.roggerexpress.service;

import com.utp.roggerexpress.dto.CambioClaveRequest;
import com.utp.roggerexpress.dto.LoginRequest;
import com.utp.roggerexpress.dto.LoginResponse;
import com.utp.roggerexpress.dto.UsuarioResponse;
import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.SesionInvalidaException;
import com.utp.roggerexpress.model.SesionRefresh;
import com.utp.roggerexpress.model.Usuario;
import com.utp.roggerexpress.repository.UsuarioRepository;
import com.utp.roggerexpress.security.ControlIntentosService;
import com.utp.roggerexpress.security.JwtService;
import com.utp.roggerexpress.security.RefreshTokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Inicio de sesion, renovacion del acceso, perfil y cambio de contrasena.
 *
 * El acceso se compone de dos piezas: un token de vida corta que viaja en cada
 * peticion y un token de refresco de vida larga que permite renovarlo. El
 * refresco se guarda en la base como hash y se rota en cada uso, de modo que la
 * empresa puede cerrar una sesion aunque el token siga en poder del cliente.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ControlIntentosService controlIntentos;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       ControlIntentosService controlIntentos,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.controlIntentos = controlIntentos;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Valida las credenciales. Antes de intentarlo comprueba que la cuenta no
     * este bloqueada por intentos fallidos, y despues actualiza ese contador:
     * asi una lista de contrasenas probadas a ciegas se detiene sola.
     */
    public LoginResponse iniciarSesion(LoginRequest request) {

        controlIntentos.verificar(request.username());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            controlIntentos.registrarFallo(request.username());
            throw ex;
        }

        controlIntentos.registrarExito(request.username());

        Usuario usuario = buscar(request.username());
        return credenciales(usuario);
    }

    /**
     * Entrega un token de acceso nuevo a cambio de un refresco valido y
     * sustituye ese refresco por otro: si alguien copio el anterior, deja de
     * servir en cuanto el usuario legitimo renueva.
     */
    public LoginResponse refrescar(String refreshToken) {

        SesionRefresh sesion = refreshTokenService.validar(refreshToken)
                .orElseThrow(() -> new SesionInvalidaException(
                        "Su sesion expiro o fue cerrada. Vuelva a iniciar sesion."));

        Usuario usuario = buscar(sesion.getUsername());

        if (!usuario.isActivo()) {
            refreshTokenService.revocarTodas(usuario.getUsername());
            throw new SesionInvalidaException(
                    "Su usuario esta desactivado. Comuniquese con el administrador.");
        }

        refreshTokenService.revocar(sesion);
        return credenciales(usuario);
    }

    /** Cierra la sesion invalidando el token de refresco en el servidor. */
    public void cerrarSesion(String refreshToken) {
        refreshTokenService.revocarPorToken(refreshToken);
    }

    public UsuarioResponse perfil(String username) {
        return UsuarioResponse.from(buscar(username));
    }

    /**
     * Cambia la contrasena del propio usuario. Exige la contrasena vigente,
     * de modo que una sesion abierta no basta para apropiarse de la cuenta, y
     * cierra las demas sesiones abiertas con la clave anterior.
     */
    public UsuarioResponse cambiarClave(String username, CambioClaveRequest request) {

        Usuario usuario = buscar(username);

        if (!passwordEncoder.matches(request.claveActual(), usuario.getPassword())) {
            throw new DatosInvalidosException("La contrasena actual no es correcta");
        }
        if (passwordEncoder.matches(request.claveNueva(), usuario.getPassword())) {
            throw new DatosInvalidosException("La nueva contrasena debe ser distinta de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(request.claveNueva()));
        usuario.setDebeCambiarClave(false);

        UsuarioResponse actualizado = UsuarioResponse.from(usuarioRepository.save(usuario));
        refreshTokenService.revocarTodas(username);

        return actualizado;
    }

    private LoginResponse credenciales(Usuario usuario) {
        return LoginResponse.de(
                jwtService.generar(usuario),
                refreshTokenService.emitir(usuario.getUsername()),
                jwtService.segundosDeVigencia(),
                UsuarioResponse.from(usuario));
    }

    private Usuario buscar(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
