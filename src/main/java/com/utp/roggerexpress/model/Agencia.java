package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Catalogo de agencias y terminales de la empresa.
 */
@Document(collection = "agencias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agencia {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String nombre;
    private String ciudad;
    private String direccion;
    private String telefono;

    @Builder.Default
    private boolean activo = true;
}
