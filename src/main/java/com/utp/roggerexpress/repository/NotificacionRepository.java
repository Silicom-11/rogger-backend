package com.utp.roggerexpress.repository;

import com.utp.roggerexpress.model.EstadoNotificacion;
import com.utp.roggerexpress.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByCodigoRastreo(String codigoRastreo);

    /** Avisos fallidos que aun no agotaron los reintentos automaticos. */
    List<Notificacion> findByEstadoAndIntentosLessThan(EstadoNotificacion estado, int intentos);

    long countByEstado(EstadoNotificacion estado);
}
