package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Documento embebido: datos del remitente o del destinatario de la encomienda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    private String nombres;
    private String dni;
    private String telefono;
}
