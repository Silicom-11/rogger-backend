package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.TransicionInvalidaException;
import com.utp.roggerexpress.model.Tarifario;
import com.utp.roggerexpress.repository.TarifarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tarifario por ruta: es lo que reemplaza el criterio manual del operador.
 */
@Service
public class TarifarioService {

    private final TarifarioRepository tarifarioRepository;

    public TarifarioService(TarifarioRepository tarifarioRepository) {
        this.tarifarioRepository = tarifarioRepository;
    }

    public List<Tarifario> listar() {
        return tarifarioRepository.findAll();
    }

    public List<Tarifario> listarVigentes() {
        return tarifarioRepository.buscarVigentes();
    }

    public Tarifario registrar(Tarifario tarifario) {
        // El nuevo tarifario desactiva al anterior de la misma ruta
        tarifarioRepository.findFirstByRutaAndActivoTrueOrderByVigenteDesdeDesc(tarifario.getRuta())
                .ifPresent(anterior -> {
                    anterior.setActivo(false);
                    tarifarioRepository.save(anterior);
                });
        return tarifarioRepository.save(tarifario);
    }

    /**
     * Corrige un tarifario ya cargado. Las encomiendas admitidas conservan el
     * importe con el que se cobraron: la tarifa se copia en el envio al
     * registrarlo, no se recalcula despues.
     */
    public Tarifario actualizar(String id, Tarifario datos) {

        Tarifario tarifario = buscar(id);

        tarifario.setMontoBase(datos.getMontoBase());
        tarifario.setPrecioPorKg(datos.getPrecioPorKg());
        tarifario.setFactorVolumetrico(datos.getFactorVolumetrico());
        tarifario.setRecargoFragil(datos.getRecargoFragil());

        return tarifarioRepository.save(tarifario);
    }

    /**
     * Elimina un tarifario cargado por error. No puede quitarse el ultimo
     * vigente de una ruta: sin tarifario esa ruta dejaria de poder cotizarse.
     */
    public void eliminar(String id) {

        Tarifario tarifario = buscar(id);

        if (tarifario.isActivo() && tarifarioRepository.contarVigentesDeRuta(tarifario.getRuta()) <= 1) {
            throw new TransicionInvalidaException(
                    "Es el único tarifario vigente de la ruta " + tarifario.getRuta()
                            + ". Cargue el nuevo antes de eliminar este.");
        }

        tarifarioRepository.delete(tarifario);
    }

    public Tarifario buscar(String id) {
        return tarifarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tarifario " + id));
    }
}
