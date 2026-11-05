package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.immobilier.service.LocationService;
import com.kupanga.api.immobilier.repository.DemandeLocationRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.DemandeLocationService;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.service.UserService;

import java.time.Clock;

/** Accès de test au constructeur avec horloge fixée de {@link DemandeLocationServiceImpl}. */
public final class DemandeLocationServiceImplAccess {

    private DemandeLocationServiceImplAccess() {
    }

    public static DemandeLocationService creer(DemandeLocationRepository demandeRepository, LocationService locationService,
                                               ConversationRepository conversationRepository, BienService bienService,
                                               UserService userService, NotificationService notificationService,
                                               EmailService emailService, Clock clock) {
        return new DemandeLocationServiceImpl(demandeRepository, locationService, conversationRepository, bienService,
                userService, notificationService, emailService, clock);
    }
}
