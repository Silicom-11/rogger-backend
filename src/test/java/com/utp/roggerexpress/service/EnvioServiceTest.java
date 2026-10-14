package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.EntregaNoPermitidaException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.TransicionInvalidaException;
import com.utp.roggerexpress.model.*;
import com.utp.roggerexpress.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la logica de encomiendas: admision, transiciones de
 * estado y entrega verificada con documento y clave de seguridad.
 */
class EnvioServiceTest {

    private EnvioRepository envioRepository;
    private TarifaService tarifaService;
    private AgenciaService agenciaService;
    private NotificacionService notificacionService;
    private MongoTemplate mongoTemplate;
    private PasswordEncoder passwordEncoder;
    private EnvioService envioService;

    @BeforeEach
    void setUp() {
        envioRepository = mock(EnvioRepository.class);
        tarifaService = mock(TarifaService.class);
        agenciaService = mock(AgenciaService.class);
        notificacionService = mock(NotificacionService.class);
        mongoTemplate = mock(MongoTemplate.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        envioService = new EnvioService(envioRepository, tarifaService, agenciaService,
                notificacionService, mongoTemplate, passwordEncoder);

        when(agenciaService.buscarPorCodigo("AG-LIM-VIC")).thenReturn(
                Agencia.builder().codigo("AG-LIM-VIC").nombre("Lima - La Victoria").ciudad("Lima").build());
        when(agenciaService.buscarPorCodigo("AG-HYO-CEN")).thenReturn(
                Agencia.builder().codigo("AG-HYO-CEN").nombre("Huancayo - Central").ciudad("Huancayo").build());
        when(tarifaService.calcular(any(), anyString())).thenReturn(
                Tarifa.builder().total(new BigDecimal("45.50")).moneda("PEN").build());
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Envio envioBase() {
        return Envio.builder()
                .remitente(new Persona("Jorge Ramos", "45871236", "987654321"))
                .destinatario(new Persona("Luisa Quispe", "70125489", "964112233"))
                .carga(Carga.builder().descripcion("Caja").pesoKg(new BigDecimal("12.5"))
                        .largoCm(40).anchoCm(30).altoCm(25).build())
                .agenciaOrigenId("AG-LIM-VIC")
                .agenciaDestinoId("AG-HYO-CEN")
                .build();
    }

    /** Encomienda ya lista para el recojo, con su clave cifrada. */
    private Envio listaParaRecojo(String clave) {
        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000200");
        envio.setEstadoActual(EstadoEnvio.LISTO_PARA_RECOJO);
        envio.setClaveRecojoHash(passwordEncoder.encode(clave));
        when(envioRepository.findByCodigoRastreo("RE-2026-000200")).thenReturn(Optional.of(envio));
        return envio;
    }

    @Test
    @DisplayName("PU-13: no se admite una encomienda sin telefono del destinatario")
    void exigeContactoDelDestinatario() {

        Envio sinContacto = envioBase();
        sinContacto.setDestinatario(new Persona("Luisa Quispe", "70125489", null));

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> envioService.registrar(sinContacto, null, "operador.lima"));

        assertTrue(ex.getMessage().toLowerCase().contains("telefono"), ex.getMessage());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    @DisplayName("PU-06: al registrar genera codigo, estado REGISTRADO y primer evento")
    void registraEncomienda() {
        when(envioRepository.count()).thenReturn(147L);
        when(envioRepository.existsByCodigoRastreo(anyString())).thenReturn(false);

        Envio registrado = envioService.registrar(envioBase(), null, "operador.lima").envio();

        assertNotNull(registrado.getCodigoRastreo());
        assertTrue(registrado.getCodigoRastreo().startsWith("RE-"));
        assertEquals(EstadoEnvio.REGISTRADO, registrado.getEstadoActual());
        assertEquals("operador.lima", registrado.getRegistradoPor());
        assertEquals(1, registrado.getHistorial().size());
        assertNotNull(registrado.getTarifa());
    }

    @Test
    @DisplayName("PU-14: la admision entrega codigo de verificacion y clave, y guarda la clave cifrada")
    void entregaCodigoDeVerificacionYClave() {
        when(envioRepository.count()).thenReturn(0L);
        when(envioRepository.existsByCodigoRastreo(anyString())).thenReturn(false);

        EnvioService.Admision admision = envioService.registrar(envioBase(), null, "operador.lima");

        assertEquals(4, admision.codigoVerificacion().length());
        assertTrue(admision.claveRecojo().matches("\\d{6}"), admision.claveRecojo());

        // En la base queda el hash, nunca la clave en claro.
        assertNotEquals(admision.claveRecojo(), admision.envio().getClaveRecojoHash());
        assertTrue(passwordEncoder.matches(admision.claveRecojo(), admision.envio().getClaveRecojoHash()));
    }

    @Test
    @DisplayName("PU-15: si el cliente dicta su clave, esa es la que se guarda")
    void respetaLaClaveDictadaPorElCliente() {
        when(envioRepository.count()).thenReturn(0L);
        when(envioRepository.existsByCodigoRastreo(anyString())).thenReturn(false);

        EnvioService.Admision admision = envioService.registrar(envioBase(), "4821", "operador.lima");

        assertEquals("4821", admision.claveRecojo());
        assertTrue(passwordEncoder.matches("4821", admision.envio().getClaveRecojoHash()));
    }

    @Test
    @DisplayName("PU-16: el rastreo exige el codigo de verificacion correcto")
    void elRastreoExigeElCodigoDeVerificacion() {

        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000300");
        envio.setCodigoVerificacion("K7QM");
        when(envioRepository.findByCodigoRastreo("RE-2026-000300")).thenReturn(Optional.of(envio));

        assertEquals("RE-2026-000300",
                envioService.rastrear("RE-2026-000300", "k7qm").getCodigoRastreo());

        assertThrows(RecursoNoEncontradoException.class,
                () -> envioService.rastrear("RE-2026-000300", "ZZZZ"));
    }

    @Test
    @DisplayName("PU-17: la entrega con documento y clave correctos queda verificada")
    void entregaConClaveCorrecta() {

        listaParaRecojo("482190");

        Entrega entrega = Entrega.builder()
                .receptorNombres("Luisa Quispe").receptorDni("70125489")
                .usuario("almacen.huancayo").build();

        Envio entregado = envioService.registrarEntrega("RE-2026-000200", entrega, "482190", null);

        assertEquals(EstadoEnvio.ENTREGADO, entregado.getEstadoActual());
        assertTrue(entregado.getEntrega().isClaveVerificada());
        assertEquals("TITULAR", entregado.getEntrega().getRelacion());
    }

    @Test
    @DisplayName("PU-18: una clave equivocada impide la entrega")
    void rechazaLaClaveEquivocada() {

        listaParaRecojo("482190");

        Entrega entrega = Entrega.builder()
                .receptorNombres("Luisa Quispe").receptorDni("70125489")
                .usuario("almacen.huancayo").build();

        EntregaNoPermitidaException ex = assertThrows(EntregaNoPermitidaException.class,
                () -> envioService.registrarEntrega("RE-2026-000200", entrega, "000000", null));

        assertTrue(ex.getMessage().toLowerCase().contains("clave"), ex.getMessage());
    }

    @Test
    @DisplayName("PU-19: sin clave solo se entrega dejando constancia del motivo")
    void sinClaveExigeMotivo() {

        listaParaRecojo("482190");

        Entrega sinMotivo = Entrega.builder()
                .receptorNombres("Luisa Quispe").receptorDni("70125489")
                .usuario("almacen.huancayo").build();

        assertThrows(EntregaNoPermitidaException.class,
                () -> envioService.registrarEntrega("RE-2026-000200", sinMotivo, null, null));

        Entrega conMotivo = Entrega.builder()
                .receptorNombres("Luisa Quispe").receptorDni("70125489")
                .motivoSinClave("El cliente no recuerda la clave; se verifico su DNI fisico")
                .usuario("almacen.huancayo").build();

        Envio entregado = envioService.registrarEntrega("RE-2026-000200", conMotivo, null, null);

        assertEquals(EstadoEnvio.ENTREGADO, entregado.getEstadoActual());
        assertFalse(entregado.getEntrega().isClaveVerificada());
        assertNotNull(entregado.getEntrega().getMotivoSinClave());
    }

    @Test
    @DisplayName("PU-20: una persona autorizada puede recoger; un tercero no")
    void soloElTitularOUnAutorizadoRecogen() {

        Envio envio = listaParaRecojo("482190");
        envio.setAutorizados(List.of(new Persona("Marco Quispe", "44556677", "955000111")));

        Entrega autorizado = Entrega.builder()
                .receptorNombres("Marco Quispe").receptorDni("44556677")
                .usuario("almacen.huancayo").build();

        Envio entregado = envioService.registrarEntrega("RE-2026-000200", autorizado, "482190", null);
        assertEquals("AUTORIZADO", entregado.getEntrega().getRelacion());

        // Un documento ajeno no puede retirarla.
        Envio otro = listaParaRecojo("482190");
        otro.setAutorizados(List.of());

        Entrega extrano = Entrega.builder()
                .receptorNombres("Alguien Mas").receptorDni("11223344")
                .usuario("almacen.huancayo").build();

        assertThrows(EntregaNoPermitidaException.class,
                () -> envioService.registrarEntrega("RE-2026-000200", extrano, "482190", null));
    }

    @Test
    @DisplayName("PU-07: la transicion valida avanza el estado y deja evento")
    void avanzaEstado() {
        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000148");
        envio.setEstadoActual(EstadoEnvio.REGISTRADO);
        when(envioRepository.findByCodigoRastreo("RE-2026-000148")).thenReturn(Optional.of(envio));

        Envio actualizado = envioService.actualizarEstado("RE-2026-000148",
                EstadoEnvio.EN_ALMACEN_ORIGEN, "AG-LIM-VIC", "almacen.lima", "Recibido");

        assertEquals(EstadoEnvio.EN_ALMACEN_ORIGEN, actualizado.getEstadoActual());
        assertEquals(1, actualizado.getHistorial().size());
    }

    @Test
    @DisplayName("PU-08: una transicion no permitida se rechaza")
    void rechazaTransicionInvalida() {
        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000149");
        envio.setEstadoActual(EstadoEnvio.REGISTRADO);
        when(envioRepository.findByCodigoRastreo("RE-2026-000149")).thenReturn(Optional.of(envio));

        assertThrows(TransicionInvalidaException.class, () -> envioService.actualizarEstado(
                "RE-2026-000149", EstadoEnvio.ENTREGADO, "AG-HYO-CEN", "almacen.huancayo", null));
    }

    @Test
    @DisplayName("PU-09: al quedar lista para recojo se avisa al destinatario")
    void avisaAlDestinatario() {
        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000150");
        envio.setEstadoActual(EstadoEnvio.EN_ALMACEN_DESTINO);
        when(envioRepository.findByCodigoRastreo("RE-2026-000150")).thenReturn(Optional.of(envio));

        envioService.actualizarEstado("RE-2026-000150", EstadoEnvio.LISTO_PARA_RECOJO,
                "AG-HYO-CEN", "almacen.huancayo", null);

        verify(notificacionService, times(1)).notificarListoParaRecojo(any(Envio.class));
    }

    @Test
    @DisplayName("PU-10: no se entrega una encomienda que no esta lista para el recojo")
    void rechazaEntregaFueraDeEstado() {
        Envio envio = envioBase();
        envio.setCodigoRastreo("RE-2026-000151");
        envio.setEstadoActual(EstadoEnvio.EN_TRANSITO);
        when(envioRepository.findByCodigoRastreo("RE-2026-000151")).thenReturn(Optional.of(envio));

        Entrega entrega = Entrega.builder()
                .receptorNombres("Luisa Quispe").receptorDni("70125489").build();

        assertThrows(EntregaNoPermitidaException.class,
                () -> envioService.registrarEntrega("RE-2026-000151", entrega, "482190", null));
    }

    @Test
    @DisplayName("PU-11: un codigo inexistente devuelve recurso no encontrado")
    void codigoInexistente() {
        when(envioRepository.findByCodigoRastreo("RE-2026-999999")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> envioService.buscarPorCodigo("RE-2026-999999"));
    }
}
