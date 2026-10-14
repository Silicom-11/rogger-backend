package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Personal que opera el sistema.
 */
@Document(collection = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;
    private String nombreCompleto;
    private Rol rol;
    private String agenciaId;

    @Builder.Default
    private boolean activo = true;

    /**
     * Marca los usuarios creados por un administrador o con la contrasena
     * restablecida: en su siguiente ingreso deben definir una propia.
     */
    @Builder.Default
    private boolean debeCambiarClave = false;
}
