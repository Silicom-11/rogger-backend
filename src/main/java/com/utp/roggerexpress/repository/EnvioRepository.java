package com.utp.roggerexpress.repository;

import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.EstadoEnvio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la coleccion de encomiendas.
 *
 * Las consultas se declaran de dos maneras. Las primeras se derivan del nombre
 * del metodo: Spring Data lee findByEstadoActual y escribe la consulta por
 * nosotros. Las que llevan @Query se escriben sobre los campos del documento
 * —no sobre nombres fisicos de tablas—, que es el papel que cumple JPQL en un
 * proyecto con base relacional.
 */
@Repository
public interface EnvioRepository extends MongoRepository<Envio, String> {

    // ---------------------------------------------------------- derivadas
    Optional<Envio> findByCodigoRastreo(String codigoRastreo);

    boolean existsByCodigoRastreo(String codigoRastreo);

    List<Envio> findByEstadoActual(EstadoEnvio estadoActual);

    List<Envio> findByAgenciaDestinoIdAndEstadoActual(String agenciaDestinoId, EstadoEnvio estadoActual);

    List<Envio> findByAgenciaDestinoId(String agenciaDestinoId);

    long countByEstadoActual(EstadoEnvio estadoActual);

    // ---------------------------------------------------------- declaradas

    /**
     * Carga fragil pendiente de entrega, que es la que exige manipulacion
     * especial en el almacen. La proyeccion devuelve solo lo que necesita la
     * lista, no el documento completo.
     */
    @Query(value = "{ 'carga.fragil': true, 'estadoActual': { $nin: ['ENTREGADO', 'ANULADO'] } }",
           fields = "{ 'codigoRastreo': 1, 'destinatario.nombres': 1, 'carga.descripcion': 1, "
                  + "'estadoActual': 1, 'agenciaDestinoId': 1 }",
           sort = "{ 'fechaRegistro': -1 }")
    List<Envio> buscarFragilesPendientes();

    /**
     * Encomiendas cuyo importe alcanza el monto indicado, de mayor a menor.
     * Sirve para revisar los envios de mayor valor de la operacion.
     */
    @Query(value = "{ 'tarifa.total': { $gte: ?0 }, 'estadoActual': { $ne: 'ANULADO' } }",
           sort = "{ 'tarifa.total': -1 }")
    List<Envio> buscarDesdeImporte(BigDecimal montoMinimo);

    /**
     * Todo lo que una persona envio o espera recibir, buscando su documento en
     * cualquiera de los dos extremos de la encomienda.
     */
    @Query(value = "{ $or: [ { 'remitente.dni': ?0 }, { 'destinatario.dni': ?0 } ] }",
           sort = "{ 'fechaRegistro': -1 }")
    List<Envio> buscarPorDocumento(String dni);

    /**
     * Encomiendas admitidas en un rango de fechas con un estado determinado.
     * Alimenta el reporte de operacion por periodo.
     */
    @Query("{ 'estadoActual': ?0, 'fechaRegistro': { $gte: ?1, $lte: ?2 } }")
    List<Envio> buscarPorEstadoYPeriodo(EstadoEnvio estado, LocalDateTime desde, LocalDateTime hasta);

    /** Cuenta cuantas encomiendas tiene una agencia, como origen o como destino. */
    @Query(value = "{ $or: [ { 'agenciaOrigenId': ?0 }, { 'agenciaDestinoId': ?0 } ] }", count = true)
    long contarPorAgencia(String agenciaId);
}
