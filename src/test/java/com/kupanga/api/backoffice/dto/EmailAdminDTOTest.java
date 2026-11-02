package com.kupanga.api.backoffice.dto;

import com.kupanga.api.email.envoi.VerificationSmtp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("M4 : adresse masquée et contrôle SMTP au démarrage")
class EmailAdminDTOTest {

    @Test
    @DisplayName("Adresse masquée : première lettre + domaine")
    void masquer() {
        assertThat(EmailAdminDTO.masquer("jean.dupont@gmail.com")).isEqualTo("j***@gmail.com");
        assertThat(EmailAdminDTO.masquer("a@b.cd")).isEqualTo("a***@b.cd");
        assertThat(EmailAdminDTO.masquer("sans-arobase")).isEqualTo("***");
        assertThat(EmailAdminDTO.masquer("@domaine.fr")).isEqualTo("***");
        assertThat(EmailAdminDTO.masquer(null)).isNull();
    }

    @Test
    @DisplayName("Page énorme ramenée dans les bornes (pas de 500 sur le décalage SQL)")
    void pageBornee() {
        EmailAdminSearchDTO dto = new EmailAdminSearchDTO(null, null, Integer.MAX_VALUE, 100);
        assertThat((long) dto.page() * dto.size()).isLessThanOrEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Serveur SMTP injoignable au démarrage → false (log ERROR), sans exception")
    void verificationSmtp_injoignable() {
        JavaMailSenderImpl smtp = new JavaMailSenderImpl();
        smtp.setHost("localhost");
        smtp.setPort(1);
        smtp.getJavaMailProperties().put("mail.smtp.connectiontimeout", "1000");

        assertThat(new VerificationSmtp(smtp).verifier()).isFalse();
    }
}
