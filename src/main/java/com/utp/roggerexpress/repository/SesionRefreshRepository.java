package com.utp.roggerexpress.repository;

import com.utp.roggerexpress.model.SesionRefresh;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionRefreshRepository extends MongoRepository<SesionRefresh, String> {

    Optional<SesionRefresh> findByTokenHash(String tokenHash);

    List<SesionRefresh> findByUsernameAndRevocadaFalse(String username);
}
