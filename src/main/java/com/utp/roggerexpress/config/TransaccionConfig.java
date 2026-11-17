package com.utp.roggerexpress.config;

import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Habilita las transacciones multi-documento de MongoDB.
 *
 * MongoDB solo admite transacciones cuando el servidor esta desplegado como
 * conjunto de replicas. En la nube (MongoDB Atlas) siempre lo esta, de modo que
 * alli se activan; en una instalacion individual de desarrollo se dejan
 * apagadas, porque declarar el gestor de transacciones contra un servidor que
 * no las soporta haria fallar toda escritura.
 *
 * El interruptor es la propiedad app.transacciones.habilitadas, que vale true
 * en el perfil de nube y false en el entorno local.
 */
@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "app.transacciones.habilitadas", havingValue = "true")
public class TransaccionConfig {

    private static final Logger log = LoggerFactory.getLogger(TransaccionConfig.class);

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient cliente,
                                                     org.springframework.boot.autoconfigure.mongo.MongoProperties propiedades) {
        return new SimpleMongoClientDatabaseFactory(cliente, propiedades.getMongoClientDatabase());
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factoria) {
        log.info("Transacciones multi-documento habilitadas: el servidor admite sesiones");
        return new MongoTransactionManager(factoria);
    }
}
