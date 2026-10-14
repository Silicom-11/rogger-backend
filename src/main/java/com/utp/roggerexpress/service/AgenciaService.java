package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.TransicionInvalidaException;
import com.utp.roggerexpress.model.Agencia;
import com.utp.roggerexpress.repository.AgenciaRepository;
import com.utp.roggerexpress.repository.EnvioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Catalogo de agencias de la red: alta, consulta, modificacion y baja.
 */
@Service
public class AgenciaService {

    private final AgenciaRepository agenciaRepository;
    private final EnvioRepository envioRepository;

    public AgenciaService(AgenciaRepository agenciaRepository, EnvioRepository envioRepository) {
        this.agenciaRepository = agenciaRepository;
        this.envioRepository = envioRepository;
    }

    public List<Agencia> listarActivas() {
        return agenciaRepository.findByActivoTrue();
    }

    public Agencia buscarPorCodigo(String codigo) {
        return agenciaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la agencia " + codigo));
    }

    public Agencia registrar(Agencia agencia) {
        if (agenciaRepository.existsByCodigo(agencia.getCodigo())) {
            throw new DatosInvalidosException("Ya existe una agencia con el codigo " + agencia.getCodigo());
        }
        return agenciaRepository.save(agencia);
    }

    /**
     * Modifica los datos de contacto de una agencia. El codigo no se cambia:
     * es la referencia que ya quedo escrita en el historial de cada encomienda.
     */
    public Agencia actualizar(String codigo, Agencia datos) {

        Agencia agencia = buscarPorCodigo(codigo);

        agencia.setNombre(datos.getNombre());
        agencia.setCiudad(datos.getCiudad());
        agencia.setDireccion(datos.getDireccion());
        agencia.setTelefono(datos.getTelefono());

        return agenciaRepository.save(agencia);
    }

    /**
     * Elimina una agencia del catalogo.
     *
     * Solo se permite si nunca movio carga: si tuviera encomiendas asociadas,
     * borrarla dejaria huerfano el historial de esos envios, que debe poder
     * reconstruirse. En ese caso corresponde desactivarla, no eliminarla.
     */
    public void eliminar(String codigo) {

        Agencia agencia = buscarPorCodigo(codigo);
        long encomiendas = envioRepository.contarPorAgencia(codigo);

        if (encomiendas > 0) {
            throw new TransicionInvalidaException(
                    "La agencia " + codigo + " tiene " + encomiendas + " encomienda(s) registradas. "
                            + "Desactívela en lugar de eliminarla: su historial debe conservarse.");
        }

        agenciaRepository.delete(agencia);
    }

    /** Baja logica: la agencia deja de ofrecerse sin perder su historial. */
    public Agencia cambiarEstado(String codigo, boolean activo) {
        Agencia agencia = buscarPorCodigo(codigo);
        agencia.setActivo(activo);
        return agenciaRepository.save(agencia);
    }
}
