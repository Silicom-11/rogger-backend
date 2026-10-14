package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Sesion de larga duracion asociada a un token de refresco.
 *
 * Solo se guarda el hash del token: si alguien accediera a la base de datos no
 * podria reutilizarlo. Cada refresco rota el token y revoca el anterior.
 */
@Document(collection = "sesiones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionRefresh {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String username;

    private LocalDateTime creadaEn;
    private LocalDateTime expiraEn;

    @Builder.Default
    private boolean revocada = false;

    public boolean estaVigente() {
        return !revocada && expiraEn != null && expiraEn.isAfter(LocalDateTime.now());
    }
}
