package com.utp.roggerexpress.security;

import com.utp.roggerexpress.model.Rol;
import com.utp.roggerexpress.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del servicio de tokens JWT.
 */
class JwtServiceTest {

    private static final String SECRETO =
            "clave-de-prueba-para-firmar-tokens-hs256-con-longitud-suficiente-2026";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 60);
    }

    private Usuario usuario() {
        return Usuario.builder().username("operador.lima").nombreCompleto("Yefrie Camposano")
                .rol(Rol.OPERADOR).agenciaId("AG-LIM-VIC").activo(true).build();
    }

    @Test
    @DisplayName("PS-01: el token generado es valido y contiene el usuario")
    void generaTokenValido() {
        String token = jwtService.generar(usuario());

        assertTrue(jwtService.esValido(token));
        assertEquals("operador.lima", jwtService.extraerUsuario(token));
    }

    @Test
    @DisplayName("PS-02: un token manipulado se rechaza")
    void rechazaTokenManipulado() {
        String token = jwtService.generar(usuario()) + "alterado";

        assertFalse(jwtService.esValido(token));
    }

    @Test
    @DisplayName("PS-03: un token ya expirado se rechaza")
    void rechazaTokenExpirado() {
        JwtService expirado = new JwtService(SECRETO, -1);

        assertFalse(expirado.esValido(expirado.generar(usuario())));
    }
}
