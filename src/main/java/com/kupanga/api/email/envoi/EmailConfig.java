package com.kupanga.api.email.envoi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** M1 : le serveur SMTP est configuré par Spring Boot ({@code spring.mail.*}), l'expéditeur par {@link EmailProperties}. */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {
}
