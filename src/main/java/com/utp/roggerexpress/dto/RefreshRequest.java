package com.utp.roggerexpress.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/v1/auth/refresh y /api/v1/auth/logout. */
public record RefreshRequest(

        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken) {
}
