package com.smartquiz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI smartQuizOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartQuiz Mobile App Backend REST API")
                        .description("Production-ready Spring Boot 3.x backend REST API for SmartQuiz Android application. " +
                                "Supports User Auth with JWT Bearer Token, Quiz Management, Dynamic Question Import via Excel (.xlsx) / CSV using Apache POI, " +
                                "Quiz Taking & Automated Scoring, and Push Notifications.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartQuiz Development Team")
                                .email("support@smartquiz.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .servers(List.of(
                        new Server().url("/").description("Current Server (Local or Cloud)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token obtained from /api/auth/login or /api/auth/register")));
    }
}
