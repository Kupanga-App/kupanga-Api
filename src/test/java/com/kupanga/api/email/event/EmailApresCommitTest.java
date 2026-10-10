package com.kupanga.api.email.event;

import com.kupanga.api.config.ApresCommit;
import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.email.service.impl.EmailServiceImpl;
import com.kupanga.api.minio.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * B11 : vrais mécanismes Spring ({@code @TransactionalEventListener}, synchronisations de transaction) avec un
 * gestionnaire de transactions factice et un exécuteur synchrone. Un e-mail ou un envoi WebSocket demandé dans
 * une transaction ne part qu'après le commit, jamais après une annulation.
 */
@SpringJUnitConfig(EmailApresCommitTest.Config.class)
@DisplayName("Tests — e-mails et envois après commit (B11)")
class EmailApresCommitTest {

    @Configuration
    @EnableTransactionManagement
    @EnableAsync
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
                @Override protected void doCommit(DefaultTransactionStatus status) { }
                @Override protected void doRollback(DefaultTransactionStatus status) { }
            };
        }

        /** Même nom que l'exécuteur de production, mais synchrone : le test n'attend pas un autre thread. */
        @Bean(name = "emailExecutor")
        TaskExecutor emailExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean BrevoEmailClient brevoClient() { return mock(BrevoEmailClient.class); }
        @Bean MinioService minioService() { return mock(MinioService.class); }

        @Bean
        EmailEnvoiListener emailEnvoiListener(BrevoEmailClient brevoClient, MinioService minioService) {
            return new EmailEnvoiListener(brevoClient, minioService);
        }

        @Bean
        EmailService emailService(ApplicationEventPublisher evenements) {
            return new EmailServiceImpl(evenements, "noreply@kupanga.test", "Kupanga",
                    "http://front/reset?token=", "http://front/login", "http://front/", registreFr());
        }

        /** J3 : montants formatés comme en France (registre bouchon). */
        private static com.kupanga.api.juridiction.JuridictionRegistry registreFr() {
            com.kupanga.api.juridiction.JuridictionRegistry registre =
                    org.mockito.Mockito.mock(com.kupanga.api.juridiction.JuridictionRegistry.class);
            org.mockito.Mockito.when(registre.formatMontant(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(i -> new com.kupanga.api.juridiction.FormatMontant(java.util.Locale.FRANCE,
                            i.getArgument(1) == null ? com.kupanga.api.juridiction.Devise.EUR : i.getArgument(1)));
            return registre;
        }
    }

    @Autowired private EmailService emailService;
    @Autowired private BrevoEmailClient brevoClient;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        reset(brevoClient);
        transaction = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("E-mail demandé dans une transaction → rien avant le commit, envoyé après")
    void email_envoyeApresCommit() {
        transaction.executeWithoutResult(statut -> {
            emailService.sendPasswordResetMail("alice@kupanga.test", "jeton");
            verifyNoInteractions(brevoClient); // pas encore : la transaction n'est pas validée
        });

        verify(brevoClient).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("Transaction annulée (exception après la demande d'e-mail) → aucun e-mail")
    void transactionAnnulee_aucunEmail() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(statut -> {
            emailService.sendWelcomeMessage("alice@kupanga.test", "Alice");
            throw new IllegalStateException("échec après l'envoi demandé");
        })).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(brevoClient);
    }

    @Test
    @DisplayName("Sans transaction → e-mail envoyé tout de suite (fallbackExecution)")
    void sansTransaction_envoiImmediat() {
        emailService.sendPasswordUpdatedConfirmation("alice@kupanga.test");

        verify(brevoClient).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("ApresCommit (envois WebSocket, POI) : après le commit, rien après une annulation, immédiat sans transaction")
    void apresCommit() {
        AtomicBoolean execute = new AtomicBoolean();
        transaction.executeWithoutResult(statut -> {
            ApresCommit.executer(() -> execute.set(true));
            assertThat(execute).isFalse();
        });
        assertThat(execute).isTrue();

        AtomicBoolean apresAnnulation = new AtomicBoolean();
        transaction.executeWithoutResult(statut -> {
            ApresCommit.executer(() -> apresAnnulation.set(true));
            statut.setRollbackOnly();
        });
        assertThat(apresAnnulation).isFalse();

        AtomicBoolean sansTransaction = new AtomicBoolean();
        ApresCommit.executer(() -> sansTransaction.set(true));
        assertThat(sansTransaction).isTrue();
    }
}
