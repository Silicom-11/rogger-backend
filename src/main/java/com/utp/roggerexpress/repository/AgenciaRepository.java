package com.utp.roggerexpress.repository;

import com.utp.roggerexpress.model.Agencia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenciaRepository extends MongoRepository<Agencia, String> {

    Optional<Agencia> findByCodigo(String codigo);

    List<Agencia> findByActivoTrue();

    boolean existsByCodigo(String codigo);
}
