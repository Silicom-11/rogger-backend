package com.utp.roggerexpress.service;

import com.utp.roggerexpress.security.AlcanceConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del aislamiento de datos entre agencias.
 */
class AlcanceConsultaTest {

    @Test
    @DisplayName("PI-22: la administracion consulta sin restriccion de agencia")
    void laAdministracionVeTodaLaRed() {
        assertNull(EnvioService.criterioAlcance(AlcanceConsulta.global()));
    }

    @Test
    @DisplayName("PI-23: el personal de agencia solo ve lo que sale o llega a su sede")
    void elPersonalSoloVeSuAgencia() {

        Criteria criterio = EnvioService.criterioAlcance(AlcanceConsulta.deAgencia("AG-HYO-CEN"));

        assertNotNull(criterio);

        String consulta = criterio.getCriteriaObject().toJson();
        assertTrue(consulta.contains("agenciaOrigenId"), consulta);
        assertTrue(consulta.contains("agenciaDestinoId"), consulta);
        assertTrue(consulta.contains("AG-HYO-CEN"), consulta);
    }
}
