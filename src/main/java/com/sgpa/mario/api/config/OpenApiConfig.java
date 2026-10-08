package com.sgpa.mario.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Documentação interactiva em /swagger-ui.html e especificação em /v3/api-docs/v1. */
@Configuration
@OpenAPIDefinition(info = @Info(
    title = "SGPA — API REST",
    version = "v1",
    description = "Sistema de venda de produtos agrícolas interprovincial. "
                + "Faça login em /api/v1/auth/login e use o accessToken em 'Authorize'."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
    }
}
