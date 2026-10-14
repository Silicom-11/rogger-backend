package com.utp.roggerexpress.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion interactiva de la API disponible en /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearer-jwt";

    @Bean
    public OpenAPI roggerExpressOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API REST - Rogger Express")
                        .version("2.0.0")
                        .description("Sistema web de gestion de encomiendas. Proyecto integrador "
                                + "del curso Desarrollo Web Integrado - UTP, seccion 27524.")
                        .contact(new Contact().name("Equipo Rogger Express - UTP")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/v1/auth/login")));
    }
}
