package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.TransicionInvalidaException;
import com.utp.roggerexpress.model.Agencia;
import com.utp.roggerexpress.model.Tarifario;
import com.utp.roggerexpress.repository.AgenciaRepository;
import com.utp.roggerexpress.repository.EnvioRepository;
import com.utp.roggerexpress.repository.TarifarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las operaciones CRUD sobre los catalogos.
 *
 * Interesa sobre todo la eliminacion: un borrado que dejara sin respaldo el
 * historial de las encomiendas, o sin tarifa una ruta, seria un error que solo
 * se notaria en operacion.
 */
class CatalogoServiceTest {

    private AgenciaRepository agenciaRepository;
    private EnvioRepository envioRepository;
    private TarifarioRepository tarifarioRepository;

    private AgenciaService agenciaService;
    private TarifarioService tarifarioService;

    @BeforeEach
    void setUp() {
        agenciaRepository = mock(AgenciaRepository.class);
        envioRepository = mock(EnvioRepository.class);
        tarifarioRepository = mock(TarifarioRepository.class);

        agenciaService = new AgenciaService(agenciaRepository, envioRepository);
        tarifarioService = new TarifarioService(tarifarioRepository);

        when(agenciaRepository.save(any(Agencia.class))).thenAnswer(i -> i.getArgument(0));
        when(tarifarioRepository.save(any(Tarifario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Agencia agencia() {
        return Agencia.builder().codigo("AG-JAU-CEN").nombre("Jauja - Centro")
                .ciudad("Jauja").direccion("Jr. Grau 185").telefono("064361200").activo(true).build();
    }

    private Tarifario tarifario() {
        return Tarifario.builder().id("t1").ruta("Lima-Huancayo")
                .montoBase(new BigDecimal("8.00")).precioPorKg(new BigDecimal("3.00"))
                .factorVolumetrico(6000).recargoFragil(new BigDecimal("5.00")).activo(true).build();
    }

    @Test
    @DisplayName("PU-21: no se registra una agencia con un codigo que ya existe")
    void rechazaCodigoRepetido() {
        when(agenciaRepository.existsByCodigo("AG-JAU-CEN")).thenReturn(true);

        assertThrows(DatosInvalidosException.class, () -> agenciaService.registrar(agencia()));
        verify(agenciaRepository, never()).save(any(Agencia.class));
    }

    @Test
    @DisplayName("PU-22: al actualizar se cambian los datos de contacto, no el codigo")
    void actualizaSinCambiarElCodigo() {
        when(agenciaRepository.findByCodigo("AG-JAU-CEN")).thenReturn(Optional.of(agencia()));

        Agencia datos = Agencia.builder().codigo("OTRO").nombre("Jauja - Terminal")
                .ciudad("Jauja").direccion("Jr. Grau 185, int. 2").telefono("064361255").build();

        Agencia actualizada = agenciaService.actualizar("AG-JAU-CEN", datos);

        assertEquals("AG-JAU-CEN", actualizada.getCodigo());
        assertEquals("Jauja - Terminal", actualizada.getNombre());
        assertEquals("064361255", actualizada.getTelefono());
    }

    @Test
    @DisplayName("PU-23: se elimina una agencia que nunca movio carga")
    void eliminaAgenciaSinCarga() {
        when(agenciaRepository.findByCodigo("AG-JAU-CEN")).thenReturn(Optional.of(agencia()));
        when(envioRepository.contarPorAgencia("AG-JAU-CEN")).thenReturn(0L);

        agenciaService.eliminar("AG-JAU-CEN");

        verify(agenciaRepository, times(1)).delete(any(Agencia.class));
    }

    @Test
    @DisplayName("PU-24: no se elimina una agencia con encomiendas: su historial debe conservarse")
    void protegeLaAgenciaConHistorial() {
        when(agenciaRepository.findByCodigo("AG-LIM-VIC")).thenReturn(Optional.of(agencia()));
        when(envioRepository.contarPorAgencia("AG-LIM-VIC")).thenReturn(17L);

        TransicionInvalidaException ex = assertThrows(TransicionInvalidaException.class,
                () -> agenciaService.eliminar("AG-LIM-VIC"));

        assertTrue(ex.getMessage().contains("17"), ex.getMessage());
        verify(agenciaRepository, never()).delete(any(Agencia.class));
    }

    @Test
    @DisplayName("PU-25: eliminar una agencia inexistente devuelve recurso no encontrado")
    void agenciaInexistente() {
        when(agenciaRepository.findByCodigo(anyString())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> agenciaService.eliminar("AG-XXX"));
    }

    @Test
    @DisplayName("PU-26: no se elimina el unico tarifario vigente de una ruta")
    void protegeElUltimoTarifarioDeLaRuta() {
        when(tarifarioRepository.findById("t1")).thenReturn(Optional.of(tarifario()));
        when(tarifarioRepository.contarVigentesDeRuta("Lima-Huancayo")).thenReturn(1L);

        assertThrows(TransicionInvalidaException.class, () -> tarifarioService.eliminar("t1"));
        verify(tarifarioRepository, never()).delete(any(Tarifario.class));
    }

    @Test
    @DisplayName("PU-27: si la ruta tiene otro tarifario vigente, el anterior puede eliminarse")
    void eliminaCuandoHayReemplazo() {
        when(tarifarioRepository.findById("t1")).thenReturn(Optional.of(tarifario()));
        when(tarifarioRepository.contarVigentesDeRuta("Lima-Huancayo")).thenReturn(2L);

        tarifarioService.eliminar("t1");

        verify(tarifarioRepository, times(1)).delete(any(Tarifario.class));
    }

    @Test
    @DisplayName("PU-28: cargar un tarifario nuevo desactiva el anterior de la misma ruta")
    void elTarifarioNuevoDesactivaAlAnterior() {
        Tarifario anterior = tarifario();
        when(tarifarioRepository.findFirstByRutaAndActivoTrueOrderByVigenteDesdeDesc("Lima-Huancayo"))
                .thenReturn(Optional.of(anterior));

        tarifarioService.registrar(tarifario());

        assertFalse(anterior.isActivo());
        verify(tarifarioRepository, times(2)).save(any(Tarifario.class));
    }
}
