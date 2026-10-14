package com.utp.roggerexpress.service;

import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.model.Envio;
import com.utp.roggerexpress.model.EstadoNotificacion;
import com.utp.roggerexpress.model.Notificacion;
import com.utp.roggerexpress.notificacion.ProveedorNotificaciones;
import com.utp.roggerexpress.notificacion.ResultadoNotificacion;
import com.utp.roggerexpress.repository.NotificacionRepository;
import com.utp.roggerexpress.security.AlcanceConsulta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aviso al cliente cuando su encomienda queda lista para el recojo.
 *
 * El despacho se delega en un proveedor intercambiable. Cada intento queda
 * registrado con su resultado: los avisos que fallan quedan en ERROR y un
 * proceso periodico los reintenta hasta el limite configurado, de modo que una
 * caida momentanea de la pasarela no deja al cliente sin enterarse.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final ProveedorNotificaciones proveedor;
    private final MongoTemplate mongoTemplate;
    private final int maximoIntentos;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               ProveedorNotificaciones proveedor,
                               MongoTemplate mongoTemplate,
                               @Value("${app.notificaciones.max-intentos:4}") int maximoIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.proveedor = proveedor;
        this.mongoTemplate = mongoTemplate;
        this.maximoIntentos = maximoIntentos;
    }

    /** Crea el aviso de "listo para recojo" y lo despacha de inmediato. */
    public Notificacion notificarListoParaRecojo(Envio envio) {

        String mensaje = String.format(
                "Rogger Express: su encomienda %s ya se encuentra en la agencia de destino y esta lista "
                        + "para el recojo. Presente su DNI. Consulte en linea con su codigo.",
                envio.getCodigoRastreo());

        Notificacion notificacion = notificacionRepository.save(Notificacion.builder()
                .envioId(envio.getId())
                .codigoRastreo(envio.getCodigoRastreo())
                .canal(proveedor.canal())
                .destino(envio.getDestinatario() == null ? null : envio.getDestinatario().getTelefono())
                .mensaje(mensaje)
                .agenciaDestinoId(envio.getAgenciaDestinoId())
                .estado(EstadoNotificacion.PENDIENTE)
                .intentos(0)
                .entregaReal(false)
                .fechaEnvio(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build());

        return despachar(notificacion);
    }

    /** Reintento manual solicitado desde la aplicacion. */
    public Notificacion reintentar(String id) {

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el aviso " + id));

        if (notificacion.getEstado() == EstadoNotificacion.ENVIADO && notificacion.isEntregaReal()) {
            return notificacion;
        }
        return despachar(notificacion);
    }

    /**
     * Reintenta automaticamente los avisos que quedaron en ERROR mientras no
     * hayan agotado los intentos permitidos.
     */
    @Scheduled(fixedDelayString = "${app.notificaciones.reintento-ms:180000}",
               initialDelayString = "${app.notificaciones.reintento-ms:180000}")
    public void reintentarPendientes() {

        List<Notificacion> fallidos =
                notificacionRepository.findByEstadoAndIntentosLessThan(EstadoNotificacion.ERROR, maximoIntentos);

        if (fallidos.isEmpty()) {
            return;
        }
        log.info("Reintentando {} aviso(s) al cliente", fallidos.size());
        fallidos.forEach(this::despachar);
    }

    /** Entrega el mensaje al proveedor y registra el resultado del intento. */
    private Notificacion despachar(Notificacion notificacion) {

        notificacion.setFechaActualizacion(LocalDateTime.now());
        notificacion.setCanal(proveedor.canal());

        if (notificacion.getDestino() == null || notificacion.getDestino().isBlank()) {
            notificacion.setEstado(EstadoNotificacion.ERROR);
            notificacion.setUltimoError("El destinatario no registro un numero de contacto");
            return notificacionRepository.save(notificacion);
        }

        notificacion.setIntentos(notificacion.getIntentos() + 1);
        ResultadoNotificacion resultado = proveedor.enviar(notificacion.getDestino(), notificacion.getMensaje());

        if (resultado.aceptado()) {
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setReferenciaProveedor(resultado.referencia());
            notificacion.setEntregaReal(proveedor.entregaReal());
            notificacion.setUltimoError(null);
        } else {
            notificacion.setEstado(EstadoNotificacion.ERROR);
            notificacion.setUltimoError(resultado.error());
            notificacion.setEntregaReal(false);
        }

        return notificacionRepository.save(notificacion);
    }

    /**
     * Bitacora visible para quien consulta: la administracion ve toda la red y
     * el personal de agencia solo los avisos de las encomiendas que le llegan.
     */
    public List<Notificacion> listar(AlcanceConsulta alcance, EstadoNotificacion estado, int limite) {

        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "fechaActualizacion")).limit(limite);

        if (estado != null) {
            query.addCriteria(Criteria.where("estado").is(estado));
        }
        if (alcance != null && !alcance.todaLaRed()) {
            query.addCriteria(Criteria.where("agenciaDestinoId").is(alcance.agenciaId()));
        }

        return mongoTemplate.find(query, Notificacion.class);
    }

    public long contarPorEstado(EstadoNotificacion estado) {
        return notificacionRepository.countByEstado(estado);
    }

    /** Intentos permitidos antes de dejar de reintentar en forma automatica. */
    public int maximoIntentos() {
        return maximoIntentos;
    }
}
