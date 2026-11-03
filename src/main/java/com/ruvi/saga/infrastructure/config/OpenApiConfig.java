package com.ruvi.saga.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sagaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Saga Orchestration API")
                .description("Saga Orchestration Pattern with automatic compensating transactions. "
                        + "Order → ReserveStock → ProcessPayment, with Compensate (revert stock) on payment failure.")
                .version("v1")
                .contact(new Contact().name("Batuhan Başay"))
                .license(new License().name("MIT")));
    }
}
