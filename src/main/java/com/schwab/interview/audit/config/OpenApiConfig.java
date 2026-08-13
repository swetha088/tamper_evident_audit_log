package com.schwab.interview.audit.config;
import io.swagger.v3.oas.models.*; import io.swagger.v3.oas.models.info.Info; import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfig { @Bean OpenAPI auditOpenApi(){return new OpenAPI().info(new Info().title("Tamper-Evident Audit Log API").version("v1").description("Append-only audit events with SHA-256 hash-chain verification."));} }
