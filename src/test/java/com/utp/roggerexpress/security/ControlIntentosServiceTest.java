package com.utp.roggerexpress.security;

import com.utp.roggerexpress.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del freno a los intentos de acceso por fuerza bruta.
 */
class ControlIntentosServiceTest {

    @Test
    @DisplayName("PI-19: la cuenta se bloquea al alcanzar el maximo de intentos fallidos")
    void bloqueaTrasElMaximoDeIntentos() {

        ControlIntentosService control = new ControlIntentosService(3, 15);

        control.registrarFallo("operador.lima");
        control.registrarFallo("operador.lima");

        // Con dos fallos todavia puede intentarlo.
        assertDoesNotThrow(() -> control.verificar("operador.lima"));

        control.registrarFallo("operador.lima");

        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> control.verificar("operador.lima"));

        assertTrue(ex.getSegundosRestantes() > 0);
        assertEquals(3, control.intentosFallidos("operador.lima"));
    }

    @Test
    @DisplayName("PI-20: un ingreso correcto reinicia el contador de intentos")
    void elIngresoCorrectoLimpiaElContador() {

        ControlIntentosService control = new ControlIntentosService(5, 15);

        control.registrarFallo("almacen.huancayo");
        control.registrarFallo("almacen.huancayo");
        assertEquals(2, control.intentosFallidos("almacen.huancayo"));

        control.registrarExito("almacen.huancayo");

        assertEquals(0, control.intentosFallidos("almacen.huancayo"));
        assertDoesNotThrow(() -> control.verificar("almacen.huancayo"));
    }

    @Test
    @DisplayName("PI-21: el bloqueo distingue mayusculas y minusculas del usuario")
    void elBloqueoNoDependeDeLasMayusculas() {

        ControlIntentosService control = new ControlIntentosService(2, 15);

        control.registrarFallo("Admin");
        control.registrarFallo("ADMIN");

        assertThrows(DemasiadosIntentosException.class, () -> control.verificar("admin"));
    }
}
