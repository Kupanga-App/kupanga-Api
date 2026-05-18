package com.kupanga.api.email.constantes;

public class Constante {

    // ─── 01 · Bienvenue (profil complété) ─────────────────────────────────────────
    public static final String SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE =
            "Bienvenue sur KUPANGA, %s !";

    // %1$s = prénom (preheader) · %2$s = prénom (h1)
    public static final String CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Bienvenue sur KUPANGA, %s ! Votre profil est complet.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Bienvenue · Profil complété
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Bienvenue sur KUPANGA,<br/>%s !
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Nous sommes ravis de vous compter parmi nous.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Votre profil a été complété avec succès. Vous pouvez désormais profiter pleinement de KUPANGA, votre application de gestion immobilière.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(143,164,100,0.15); border-left:3px solid #8FA464;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#4F5F37; line-height:1.55;">
                    Votre espace personnel est prêt.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 02 · Réinitialisation mot de passe ─────────────────────────────────────────
    public static final String SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE =
            "Réinitialisation de votre mot de passe – KUPANGA";

    // %s = lien de réinitialisation
    public static final String CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Lien de réinitialisation de votre mot de passe — valable 10 minutes.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Sécurité du compte
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Réinitialisation de mot de passe – KUPANGA
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Vous avez demandé la réinitialisation de votre mot de passe.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(212,168,83,0.14); border-left:3px solid #D4A853;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#7A5E22; line-height:1.55;">
                    Pour des raisons de sécurité, ce lien est valable <strong>10 minutes</strong>.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:18px 32px 14px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="background:#8FA464; border-radius:8px;">
                    <a href="%s" style="display:inline-block; padding:14px 28px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; font-weight:600; color:#10212B; text-decoration:none; letter-spacing:0.01em;">
                      Réinitialiser mon mot de passe
                    </a>
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:12px; line-height:1.65; color:#6B7984; font-weight:300;">
                  Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 03 · Confirmation mot de passe mis à jour ─────────────────────────────────────────
    public static final String SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE =
            "Confirmation de mise à jour du mot de passe – KUPANGA";

    // %s = lien de connexion
    public static final String CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Le mot de passe de votre compte KUPANGA a été mis à jour avec succès.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Confirmation · Sécurité
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Mot de passe mis à jour avec succès
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Bonjour,
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Nous vous confirmons que le mot de passe de votre compte <strong>KUPANGA</strong> a été mis à jour avec succès.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(143,164,100,0.15); border-left:3px solid #8FA464;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#4F5F37; line-height:1.55;">
                    Votre compte est désormais sécurisé avec votre nouveau mot de passe.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Si vous êtes à l'origine de cette modification, aucune action supplémentaire n'est requise.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(192,97,74,0.13); border-left:3px solid #C0614A;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#8A3F2A; line-height:1.55;">
                    Si vous n'êtes pas à l'origine de ce changement, veuillez contacter immédiatement notre support.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:18px 32px 14px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="background:#8FA464; border-radius:8px;">
                    <a href="%s" style="display:inline-block; padding:14px 28px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; font-weight:600; color:#10212B; text-decoration:none; letter-spacing:0.01em;">
                      Se connecter
                    </a>
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 04 · Invitation à signer un contrat ─────────────────────────────────────────
    public static final String SUJET_MAIL_INVITATION_SIGNATURE =
            "KUPANGA — Vous avez un contrat à signer";

    // %1$s = nom propriétaire (preheader) · %2$s = prénom locataire · %3$s = nom propriétaire
    // %4$s = adresse · %5$s = loyer · %6$s = charges · %7$s = dépôt · %8$s = date début
    // %9$s = durée · %10$s = lien signature
    public static final String CONTENU_MAIL_INVITATION_SIGNATURE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">%1$s vous invite à signer votre contrat de location.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Invitation à signer
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Bonjour %2$s,
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  <strong>%3$s</strong>, propriétaire du bien situé au <strong>%4$s</strong>, vous invite à signer votre contrat de location.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:8px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; border:1px solid rgba(143,164,100,0.20); border-radius:10px;">
                  <tr><td style="padding:6px 18px 10px;">
                    <div style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.25em; text-transform:uppercase; color:#6B7C4B; padding:8px 0 4px;">
                      Récapitulatif du contrat
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Loyer mensuel</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%5$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Charges mensuelles</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%6$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Dépôt de garantie</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%7$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Date de début</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%8$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; font-weight:400;">Durée du bail</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0;"><strong>%9$s mois</strong></td>
                  </tr></table>
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Pour signer votre contrat, cliquez sur le bouton ci-dessous :
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:18px 32px 14px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="background:#8FA464; border-radius:8px;">
                    <a href="%10$s" style="display:inline-block; padding:14px 28px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; font-weight:600; color:#10212B; text-decoration:none; letter-spacing:0.01em;">
                      Signer le contrat
                    </a>
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(212,168,83,0.14); border-left:3px solid #D4A853;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#7A5E22; line-height:1.55;">
                    Ce lien est valable <strong>72 heures</strong>. Passé ce délai, vous devrez contacter le propriétaire pour obtenir un nouveau lien.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 05 · Contrat signé par les deux parties ─────────────────────────────────────────
    public static final String SUJET_MAIL_CONTRAT_SIGNE =
            "KUPANGA — Votre contrat a été signé par les deux parties";

    // %1$s = prénom/nom destinataire · %2$s = adresse · %3$s = loyer · %4$s = charges
    // %5$s = dépôt · %6$s = date début · %7$s = durée
    public static final String CONTENU_MAIL_CONTRAT_SIGNE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Votre contrat de location a été signé par les deux parties.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Contrat finalisé
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Contrat signé avec succès
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Bonjour <strong>%1$s</strong>,
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Votre contrat de location concernant le bien situé au <strong>%2$s</strong> a été signé par les deux parties.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:8px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; border:1px solid rgba(143,164,100,0.20); border-radius:10px;">
                  <tr><td style="padding:6px 18px 10px;">
                    <div style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.25em; text-transform:uppercase; color:#6B7C4B; padding:8px 0 4px;">
                      Récapitulatif du contrat
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Loyer mensuel</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%3$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Charges mensuelles</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%4$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Dépôt de garantie</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%5$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Date de début</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%6$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; font-weight:400;">Durée du bail</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0;"><strong>%7$s mois</strong></td>
                  </tr></table>
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Le contrat signé est disponible en pièce jointe de cet email. Conservez-le précieusement.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(143,164,100,0.15); border-left:3px solid #8FA464;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#4F5F37; line-height:1.55;">
                    Ce contrat a valeur de document officiel entre les deux parties.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 06 · Invitation à signer un état des lieux ─────────────────────────────────────────
    public static final String SUJET_MAIL_INVITATION_SIGNATURE_EDL =
            "KUPANGA — Vous avez un état des lieux à signer";

    // %1$s = prénom/nom locataire · %2$s = prénom/nom propriétaire · %3$s = adresse
    // %4$s = type EDL · %5$s = date réalisation · %6$s = lien signature
    public static final String CONTENU_MAIL_INVITATION_SIGNATURE_EDL =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">%2$s vous invite à signer l'état des lieux.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Invitation à signer · État des lieux
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Bonjour %1$s,
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  <strong>%2$s</strong>, propriétaire du bien situé au <strong>%3$s</strong>, vous invite à signer l'état des lieux établi contradictoirement entre vous.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:8px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; border:1px solid rgba(143,164,100,0.20); border-radius:10px;">
                  <tr><td style="padding:6px 18px 10px;">
                    <div style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.25em; text-transform:uppercase; color:#6B7C4B; padding:8px 0 4px;">
                      Récapitulatif
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Type d'état des lieux</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%4$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Date de réalisation</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%5$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; font-weight:400;">Adresse du bien</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0;"><strong>%3$s</strong></td>
                  </tr></table>
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Pour signer l'état des lieux, cliquez sur le bouton ci-dessous :
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:18px 32px 14px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="background:#8FA464; border-radius:8px;">
                    <a href="%6$s" style="display:inline-block; padding:14px 28px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; font-weight:600; color:#10212B; text-decoration:none; letter-spacing:0.01em;">
                      Signer l'état des lieux
                    </a>
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(212,168,83,0.14); border-left:3px solid #D4A853;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#7A5E22; line-height:1.55;">
                    Ce lien est valable <strong>72 heures</strong>. Passé ce délai, vous devrez contacter le propriétaire pour obtenir un nouveau lien.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 07 · État des lieux signé ─────────────────────────────────────────
    public static final String SUJET_MAIL_EDL_SIGNE =
            "KUPANGA — Votre état des lieux a été signé par les deux parties";

    // %1$s = prénom/nom destinataire · %2$s = adresse · %3$s = type EDL · %4$s = date réalisation
    public static final String CONTENU_MAIL_EDL_SIGNE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Votre état des lieux a été signé par les deux parties.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    État des lieux finalisé
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  État des lieux signé avec succès
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Bonjour <strong>%1$s</strong>,
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  L'état des lieux concernant le bien situé au <strong>%2$s</strong> a été signé par les deux parties.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:8px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; border:1px solid rgba(143,164,100,0.20); border-radius:10px;">
                  <tr><td style="padding:6px 18px 10px;">
                    <div style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.25em; text-transform:uppercase; color:#6B7C4B; padding:8px 0 4px;">
                      Récapitulatif
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Type d'état des lieux</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%3$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Date de réalisation</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%4$s</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; font-weight:400;">Adresse du bien</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0;"><strong>%2$s</strong></td>
                  </tr></table>
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  L'état des lieux signé est disponible en pièce jointe de cet email. Conservez-le précieusement — il constitue la référence en cas de litige à la fin de la location.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(143,164,100,0.15); border-left:3px solid #8FA464;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#4F5F37; line-height:1.55;">
                    Ce document a valeur de preuve contradictoire entre les deux parties.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

    // ─── 08 · Quittance de loyer disponible ─────────────────────────────────────────
    public static final String SUJET_MAIL_QUITTANCE =
            "KUPANGA — Votre quittance de loyer est disponible";

    // %1$s = prénom/nom locataire · %2$s = mois + année · %3$s = adresse
    // %4$s = loyer HC · %5$s = charges · %6$s = total · %7$s = date paiement
    public static final String CONTENU_MAIL_QUITTANCE =
            """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0; padding:0; background:#FAFAF4; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;">
            <div style="display:none; max-height:0; overflow:hidden;">Quittance de loyer %2$s disponible.</div>

            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#FFFFFF; border:1px solid rgba(143,164,100,0.18);">

              <tr><td style="background:#10212B; padding:18px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="vertical-align:middle;">
                    <span style="display:inline-block; width:10px; height:10px; background:#8FA464; vertical-align:middle; margin-right:10px;"></span>
                    <span style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:22px; color:#EFFBDB; vertical-align:middle; letter-spacing:-0.01em; line-height:1;">kupanga</span>
                  </td>
                  <td style="text-align:right; vertical-align:middle; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.22em; text-transform:uppercase; color:#B5CA8D;">
                    Gestion locative
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:32px 32px 0;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.28em; text-transform:uppercase; color:#6B7C4B; padding-bottom:10px; border-bottom:1px solid #8FA464;">
                    Quittance disponible
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:20px 32px 4px;">
                <h1 style="font-family:'DM Serif Display', Georgia, 'Times New Roman', serif; font-size:30px; line-height:1.15; margin:0; color:#10212B; font-weight:400; letter-spacing:-0.01em;">
                  Votre quittance de loyer
                </h1>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Bonjour <strong>%1$s</strong>,
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  Votre propriétaire vous adresse la quittance de loyer pour le mois de <strong>%2$s</strong> concernant le logement situé au <strong>%3$s</strong>.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:8px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#FAFAF4; border:1px solid rgba(143,164,100,0.20); border-radius:10px;">
                  <tr><td style="padding:6px 18px 10px;">
                    <div style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9.5px; letter-spacing:0.25em; text-transform:uppercase; color:#6B7C4B; padding:8px 0 4px;">
                      Récapitulatif
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Loyer mensuel hors charges</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%4$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Charges mensuelles</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%5$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35); font-weight:400;">Total encaissé</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0; border-bottom:1px dotted rgba(143,164,100,0.35);"><strong>%6$s €</strong></td>
                  </tr>
                  <tr>
                    <td style="font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; padding:8px 0; font-weight:400;">Date de paiement</td>
                    <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:13px; color:#10212B; font-weight:500; text-align:right; padding:8px 0;"><strong>%7$s</strong></td>
                  </tr></table>
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <p style="margin:0; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:14px; line-height:1.65; color:#3A4F5C; font-weight:300;">
                  La quittance est disponible en pièce jointe de cet email. Conservez-la précieusement — elle constitue la preuve de votre paiement.
                </p>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:10px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:rgba(143,164,100,0.10); border-left:3px solid #8FA464;">
                  <tr><td style="padding:12px 16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#4F5F37; line-height:1.55;">
                    Cette quittance a valeur de reçu officiel conformément à l'article 21 de la loi du 6 juillet 1989.
                  </td></tr>
                </table>
              </td></tr>

              <tr><td style="background:#FFFFFF; padding:28px 32px 32px;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="border-top:1px solid rgba(143,164,100,0.20); padding-top:16px; font-family:'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:13px; color:#3A4F5C; line-height:1.7; font-weight:300;">
                    À très bientôt sur KUPANGA,<br/>L'équipe KUPANGA
                  </td>
                </tr></table>
              </td></tr>

              <tr><td style="background:#10212B; padding:16px 32px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td style="font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#B5CA8D;">
                    <span style="display:inline-block; width:5px; height:5px; background:#8FA464; margin-right:8px; vertical-align:middle;"></span>
                    <span style="color:#EFFBDB; vertical-align:middle;">Kupanga</span><span style="vertical-align:middle;"> · Gestion locative</span>
                  </td>
                  <td style="text-align:right; font-family:'DM Mono', 'Consolas', 'Courier New', monospace; font-size:9px; letter-spacing:0.20em; text-transform:uppercase; color:#6B7C4B;">
                    kupanga.fr
                  </td>
                </tr></table>
              </td></tr>
                </table>
              </td></tr>
            </table>
            </body>
            </html>
            """;

}
