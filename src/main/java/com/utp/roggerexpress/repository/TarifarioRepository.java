package com.utp.roggerexpress.repository;

import com.utp.roggerexpress.model.Tarifario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifarioRepository extends MongoRepository<Tarifario, String> {

    Optional<Tarifario> findFirstByRutaAndActivoTrueOrderByVigenteDesdeDesc(String ruta);

    /** Tarifarios vigentes, ordenados por ruta: es lo que se muestra al administrar. */
    @Query(value = "{ 'activo': true }", sort = "{ 'ruta': 1 }")
    List<Tarifario> buscarVigentes();

    /** Cuantos tarifarios vigentes quedan para una ruta. */
    @Query(value = "{ 'ruta': ?0, 'activo': true }", count = true)
    long contarVigentesDeRuta(String ruta);
}
