package com.kupanga.api.email.service;

public interface EmailService {

    void envoyerMailMotDePasseTemporaire(String destinataire, String password);

    void envoyerMailNouveauCompteFinaliser(String destinataire, String nom, String prenom, String email);
}