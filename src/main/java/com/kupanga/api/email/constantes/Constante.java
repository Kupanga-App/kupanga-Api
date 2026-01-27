package com.kupanga.api.email.constantes;

public class Constante {

    public static String SUJET_MAIL_CONFIRMATION_CREATION_COMPTE =
            "Bienvenue sur KUPANGA - Confirmation de création de votre compte";

    public static String CONTENU_MAIL_CONFIRMATION_CREATION_COMPTE =
            """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <div style="background-color: #f8fafc; padding: 20px;">
                            <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                                <h2 style="color: #2563eb;">Bienvenue sur KUPANGA, Votre application de Gestion Immobilière !</h2>
                                <p>Votre compte a été créé avec succès.</p>
                                <p>Vous pouvez dès à présent vous connecter à votre espace personnel pour compléter votre profil et commencer à utiliser l’application.</p>
                                <p>Si vous avez oublié votre mot de passe ou souhaitez en définir un nouveau, vous pouvez utiliser la fonctionnalité <strong>« Mot de passe oublié »</strong> depuis la page de connexion.</p>
                                <a href="https://kupanga.lespacelibellule.com" style="display: inline-block; background-color: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Se connecter</a>
                            </div>
                        </div>
                    </body>
                </html>
                """;

    public static String SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE =
            "Réinitialisation de votre mot de passe – KUPANGA";

    public static String CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE =
            """
            <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="background-color: #f8fafc; padding: 20px;">
                        <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                            <h2 style="color: #2563eb;">
                                Réinitialisation de mot de passe – KUPANGA
                            </h2>
    
                            <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
    
                            <p>
                                Pour des raisons de sécurité, ce lien est valable
                                <strong>15 minutes</strong>.
                            </p>
    
                            <div style="background-color: #eff6ff; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="margin: 0;">
                                    Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :
                                </p>
                            </div>
    
                            <a href="%s"
                               style="display: inline-block; background-color: #2563eb; color: white; padding: 12px 25px;
                                      text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Réinitialiser mon mot de passe
                            </a>
    
                            <p style="margin-top: 25px; font-size: 0.9em; color: #6b7280;">
                                Si vous n’êtes pas à l’origine de cette demande, ignorez simplement cet email.
                            </p>
                        </div>
                    </div>
                </body>
            </html>
            """;

    public static final String RESET_LINK =
            "https://kupanga.lespacelibellule.com/setNewPassword" ;

    public static String SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE =
            "Confirmation de mise à jour du mot de passe – KUPANGA";

    public static String CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE =
            """
            <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="background-color: #f8fafc; padding: 20px;">
                        <div style="background-color: white; padding: 30px; border-radius: 10px;
                                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
    
                            <h2 style="color: #16a34a;">
                                Mot de passe mis à jour avec succès
                            </h2>
    
                            <p>Bonjour,</p>
    
                            <p>
                                Nous vous confirmons que le mot de passe de votre compte
                                <strong>KUPANGA</strong> a été mis à jour avec succès.
                            </p>
    
                            <div style="background-color: #ecfdf5; padding: 15px; border-radius: 5px;
                                        margin: 20px 0;">
                                <p style="margin: 0;">
                                    ✔ Votre compte est désormais sécurisé avec votre nouveau mot de passe.
                                </p>
                            </div>
    
                            <p>
                                Si vous êtes à l’origine de cette modification, aucune action supplémentaire
                                n’est requise.
                            </p>
    
                            <p style="color: #dc2626; font-weight: bold;">
                                ⚠ Si vous n’êtes pas à l’origine de ce changement, veuillez contacter
                                immédiatement notre support.
                            </p>
    
                            <a href="https://kupanga.lespacelibellule.com"
                               style="display: inline-block; background-color: #2563eb; color: white;
                                      padding: 12px 25px; text-decoration: none; border-radius: 5px;
                                      font-weight: bold;">
                                Se connecter
                            </a>
    
                            <p style="margin-top: 25px; font-size: 0.9em; color: #6b7280;">
                                L’équipe KUPANGA
                            </p>
                        </div>
                    </div>
                </body>
            </html>
            """;
}
