package com.kupanga.api.email.constantes;

public class Constante {


    public static String SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE =
            "Bienvenue sur KUPANGA, %s !";

    public static String CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE =
            """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <div style="background-color: #f8fafc; padding: 20px;">
                            <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                                <h2 style="color: #2563eb;">
                                    Bienvenue sur KUPANGA, %s !
                                </h2>
    
                                <p>Nous sommes ravis de vous compter parmi nous 🎉</p>
    
                                <p>
                                    Votre profil a été complété avec succès.
                                    Vous pouvez désormais profiter pleinement de KUPANGA,
                                    votre application de gestion immobilière.
                                </p>
    
                               <p>
                                       Votre espace personnel est prêt.
                               </p>
    
                                <p style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                                    À très bientôt sur KUPANGA,<br/>
                                    L’équipe KUPANGA
                                </p>
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
                                <strong>10 minutes</strong>.
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
    
                            <a href="%s"
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


    public static String SUJET_MAIL_INVITATION_SIGNATURE =
            "KUPANGA — Vous avez un contrat à signer";

    public static String SUJET_MAIL_CONTRAT_SIGNE =
            "KUPANGA — Votre contrat a été signé par les deux parties";


    public static String CONTENU_MAIL_INVITATION_SIGNATURE =
            """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <div style="background-color: #f8fafc; padding: 20px;">
                            <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
    
                                <h2 style="color: #2563eb;">
                                    Bonjour %s,
                                </h2>
    
                                <p>
                                    <strong>%s</strong>, propriétaire du bien situé au
                                    <strong>%s</strong>, vous invite à signer votre contrat de location.
                                </p>
    
                                <p>
                                    <strong>Récapitulatif du contrat :</strong>
                                </p>
                                <ul style="color: #374151;">
                                    <li>Loyer mensuel : <strong>%s €</strong></li>
                                    <li>Charges mensuelles : <strong>%s €</strong></li>
                                    <li>Dépôt de garantie : <strong>%s €</strong></li>
                                    <li>Date de début : <strong>%s</strong></li>
                                    <li>Durée du bail : <strong>%s mois</strong></li>
                                </ul>
    
                                <p>Pour signer votre contrat, cliquez sur le bouton ci-dessous :</p>
    
                                <div style="text-align: center; margin: 30px 0;">
                                    <a href="%s"
                                       style="background-color: #2563eb;
                                              color: white;
                                              padding: 14px 28px;
                                              border-radius: 8px;
                                              text-decoration: none;
                                              font-weight: bold;
                                              font-size: 16px;">
                                        Signer le contrat
                                    </a>
                                </div>
    
                                <p style="color: #ef4444; font-size: 13px;">
                                    ⚠️ Ce lien est valable <strong>72 heures</strong>.
                                    Passé ce délai, vous devrez contacter le propriétaire
                                    pour obtenir un nouveau lien.
                                </p>
    
                                <p style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                                    À très bientôt sur KUPANGA,<br/>
                                    L'équipe KUPANGA
                                </p>
                            </div>
                        </div>
                    </body>
                </html>
                """;

    public static String CONTENU_MAIL_CONTRAT_SIGNE =
            """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <div style="background-color: #f8fafc; padding: 20px;">
                            <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
    
                                <h2 style="color: #16a34a;">
                                    ✅ Contrat signé avec succès !
                                </h2>
    
                                <p>Bonjour <strong>%s</strong>,</p>
    
                                <p>
                                    Votre contrat de location concernant le bien situé au
                                    <strong>%s</strong> a été signé par les deux parties.
                                </p>
    
                                <p>
                                    <strong>Récapitulatif du contrat :</strong>
                                </p>
                                <ul style="color: #374151;">
                                    <li>Loyer mensuel : <strong>%s €</strong></li>
                                    <li>Charges mensuelles : <strong>%s €</strong></li>
                                    <li>Dépôt de garantie : <strong>%s €</strong></li>
                                    <li>Date de début : <strong>%s</strong></li>
                                    <li>Durée du bail : <strong>%s mois</strong></li>
                                </ul>
    
                                <p>
                                    Le contrat signé est disponible en pièce jointe de cet email.
                                    Conservez-le précieusement.
                                </p>
    
                                <div style="background-color: #f0fdf4;
                                            border-left: 4px solid #16a34a;
                                            padding: 12px 16px;
                                            border-radius: 4px;
                                            margin: 20px 0;">
                                    <p style="margin: 0; color: #15803d;">
                                        Ce contrat a valeur de document officiel entre les deux parties.
                                    </p>
                                </div>
    
                                <p style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                                    À très bientôt sur KUPANGA,<br/>
                                    L'équipe KUPANGA
                                </p>
                            </div>
                        </div>
                    </body>
                </html>
                """;
}
