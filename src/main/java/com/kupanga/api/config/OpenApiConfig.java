package com.kupanga.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI KupangaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kupanga API")
                        .description("Documentation interactive de l’API Kupanga. "
                                + "Utilisez cette interface pour tester et explorer les endpoints REST.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Support de l'API Kupanga")
                                .url("mailto:noreplydevback@gmail.com"))

                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
