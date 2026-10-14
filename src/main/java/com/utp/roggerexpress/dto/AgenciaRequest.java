package com.utp.roggerexpress.dto;

import com.utp.roggerexpress.model.Agencia;
import jakarta.validation.constraints.NotBlank;

public record AgenciaRequest(

        @NotBlank(message = "El codigo de la agencia es obligatorio")
        String codigo,

        @NotBlank(message = "El nombre de la agencia es obligatorio")
        String nombre,

        @NotBlank(message = "La ciudad es obligatoria")
        String ciudad,

        String direccion,
        String telefono) {

    public Agencia toEntity() {
        return Agencia.builder()
                .codigo(codigo)
                .nombre(nombre)
                .ciudad(ciudad)
                .direccion(direccion)
                .telefono(telefono)
                .activo(true)
                .build();
    }
}
