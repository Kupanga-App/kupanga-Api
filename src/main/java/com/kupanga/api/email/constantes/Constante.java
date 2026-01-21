package com.kupanga.api.email.constantes;

public class Constante {

    public static String SUJET_MAIL_MOT_DE_PASSE_TEMPORAIRE =
            "Bienvenue sur KUPANGA - Vos identifiants de connexion temporaire";
    public static String CONTENU_MAIL_MOT_DE_PASSE_TEMPORAIRE =
            """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <div style="background-color: #f8fafc; padding: 20px;">
                            <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                                <h2 style="color: #2563eb;">Bienvenue sur KUPANGA, Votre application de Gestion Immobilière !</h2>
                                <p>Votre compte a été créé avec succès.</p>
                                <p>Voici votre mot de passe temporaire pour vous connecter et finaliser votre inscription :</p>
                                <div style="background-color: #eff6ff; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                    <p style="margin: 0;"><strong>Mot de passe :</strong> <span style="font-family: monospace; font-size: 1.2em;">%s</span></p>
                                </div>
                                <p>Connectez-vous dès maintenant pour compléter votre profil.</p>
                                <a href="http://localhost:8081/login" style="display: inline-block; background-color: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Se connecter</a>
                            </div>
                        </div>
                    </body>
                </html>
                """;
}
