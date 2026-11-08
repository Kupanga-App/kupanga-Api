package com.kupanga.api.email.constantes;

/**
 * Modèles des e-mails transactionnels (refonte du 8 novembre 2026, alignée sur l'interface : Figtree, olive #4f6a2e).
 * Remplis par String.format : tout % littéral s'écrit %%.
 * {{URL_APP}} et {{DOMAINE_APP}} sont remplacés par EmailServiceImpl#finaliser (domaine paramétrable).
 */
public class Constante {

    // ─── 01 · bienvenue ─────────────────────────────
    public static final String SUJET_MAIL_BIENVENUE_PROFIL_COMPLETE =
            "Bienvenue sur Kupanga, %s";

    // %1$s = prénom (preheader) · %2$s = prénom (h1)
    public static final String CONTENU_MAIL_BIENVENUE_PROFIL_COMPLETE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Votre profil est complet, votre espace Kupanga est prêt.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#e5f1e8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#22663a;"><span style="color:#2f8a4e;">&#9679;</span>&nbsp;Profil complété</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Bienvenue, %2$s</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Votre profil est complet. Avec le même compte, vous pouvez chercher un logement, publier un bien et suivre vos documents de location.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="{{URL_APP}}" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Ouvrir Kupanga</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 02 · verification-email ─────────────────────────────
    public static final String SUJET_MAIL_VERIFICATION_EMAIL =
            "Confirmez votre adresse e-mail · Kupanga";

    // %1$s = prénom (échappé) · %2$s = lien de confirmation
    public static final String CONTENU_MAIL_VERIFICATION_EMAIL =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Confirmez votre adresse pour activer votre compte. Lien valable 24 heures.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#edefe8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#46545c;"><span style="color:#66737a;">&#9679;</span>&nbsp;Activation du compte</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Confirmez votre adresse e-mail</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Votre compte Kupanga est presque prêt. Confirmez que cette adresse vous appartient pour pouvoir vous connecter.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%2$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Confirmer mon adresse</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Ce lien est valable 24 heures.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :<br><a href="%2$s" class="kp-acc" style="color:#4f6a2e; word-break:break-all;">%2$s</a></p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:20px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Vous n'avez pas créé de compte Kupanga ? Ignorez cet e-mail : aucun compte ne sera activé.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 03 · tentative-inscription ─────────────────────────────
    public static final String SUJET_MAIL_TENTATIVE_INSCRIPTION =
            "Une inscription a été tentée avec votre adresse · Kupanga";

    // %1$s = lien « mot de passe oublié »
    public static final String CONTENU_MAIL_TENTATIVE_INSCRIPTION =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Quelqu'un a voulu créer un compte avec votre adresse. Aucun compte n'a été créé.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#edefe8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#46545c;"><span style="color:#66737a;">&#9679;</span>&nbsp;Sécurité</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Vous avez déjà un compte Kupanga</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Quelqu'un vient de demander la création d'un compte avec cette adresse. Un compte existe déjà : aucun nouveau compte n'a été créé, et votre mot de passe n'a pas changé.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Si c'est vous, choisissez un nouveau mot de passe pour retrouver l'accès à votre compte.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%1$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Choisir un nouveau mot de passe</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:20px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Ce n'est pas vous ? Ignorez cet e-mail, il n'y a rien à faire.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 04 · reinitialisation-mot-de-passe ─────────────────────────────
    public static final String SUJET_MAIL_REINITIALISATION_MOT_DE_PASSE =
            "Réinitialisation de votre mot de passe · Kupanga";

    // %1$s = lien de réinitialisation
    public static final String CONTENU_MAIL_REINITIALISATION_MOT_DE_PASSE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Lien pour choisir un nouveau mot de passe, valable 10 minutes.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#edefe8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#46545c;"><span style="color:#66737a;">&#9679;</span>&nbsp;Sécurité</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Choisissez un nouveau mot de passe</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Vous avez demandé à réinitialiser le mot de passe de votre compte Kupanga.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%1$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Choisir un nouveau mot de passe</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
          <td class="kp-warn" style="background:#fbefdd; border-radius:12px; padding:14px 16px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:14px; line-height:20px; color:#8a5a12;">Ce lien est valable 10 minutes. Passé ce délai, faites une nouvelle demande depuis la page de connexion.</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :<br><a href="%1$s" class="kp-acc" style="color:#4f6a2e; word-break:break-all;">%1$s</a></p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:20px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Vous n'avez rien demandé ? Ignorez cet e-mail : votre mot de passe reste inchangé.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 05 · mot-de-passe-modifie ─────────────────────────────
    public static final String SUJET_MAIL_CONFIRMATION_MOT_DE_PASSE =
            "Votre mot de passe a été modifié · Kupanga";

    // %1$s = lien de connexion
    public static final String CONTENU_MAIL_CONFIRMATION_MOT_DE_PASSE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Le mot de passe de votre compte Kupanga vient d'être changé.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#e5f1e8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#22663a;"><span style="color:#2f8a4e;">&#9679;</span>&nbsp;Mot de passe modifié</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre mot de passe a été modifié</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Le mot de passe de votre compte Kupanga vient d'être changé. Si c'est vous, il n'y a rien d'autre à faire.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
          <td class="kp-warn" style="background:#fbefdd; border-radius:12px; padding:14px 16px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:14px; line-height:20px; color:#8a5a12;">Ce n'est pas vous ? Choisissez tout de suite un nouveau mot de passe avec « Mot de passe oublié » sur la page de connexion, puis contactez notre support.</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%1$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Se connecter</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 06 · invitation-signature-bail ─────────────────────────────
    public static final String SUJET_MAIL_INVITATION_SIGNATURE =
            "Votre bail est prêt à signer · Kupanga";

    // %1$s = nom propriétaire (preheader) · %2$s = prénom locataire · %3$s = nom propriétaire
    // %4$s = adresse · %5$s = loyer · %6$s = charges · %7$s = dépôt · %8$s = date début (formatée)
    // %9$s = durée · %10$s = lien signature
    public static final String CONTENU_MAIL_INVITATION_SIGNATURE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">%1$s vous invite à signer votre bail.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#fbefdd; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#8a5a12;"><span style="color:#b7791f;">&#9679;</span>&nbsp;À signer</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre bail est prêt à signer</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %2$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;"><strong>%3$s</strong> vous invite à signer le bail de votre logement. Vous signez à l'écran, au doigt ou à la souris, sans créer de compte.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Loyer mensuel</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%5$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Charges mensuelles</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%6$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Dépôt de garantie</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%7$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Début du bail</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%8$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Durée</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%9$s mois</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%10$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Lire et signer le bail</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
          <td class="kp-warn" style="background:#fbefdd; border-radius:12px; padding:14px 16px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:14px; line-height:20px; color:#8a5a12;">Ce lien est personnel et valable 72 heures. Passé ce délai, demandez-en un nouveau à %3$s.</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :<br><a href="%10$s" class="kp-acc" style="color:#4f6a2e; word-break:break-all;">%10$s</a></p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 07 · bail-signe ─────────────────────────────
    public static final String SUJET_MAIL_CONTRAT_SIGNE =
            "Votre bail est signé · Kupanga";

    // %1$s = prénom/nom destinataire · %2$s = adresse · %3$s = loyer · %4$s = charges
    // %5$s = dépôt · %6$s = date début (formatée) · %7$s = durée
    // %8$s = NOUVEAU : bloc « Document joint » (BLOC_PIECE_JOINTE rempli, ou chaîne vide sans PDF)
    public static final String CONTENU_MAIL_CONTRAT_SIGNE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Votre bail est signé par les deux parties.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#e5f1e8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#22663a;"><span style="color:#2f8a4e;">&#9679;</span>&nbsp;Signé</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre bail est signé</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Le bail est signé par les deux parties. Il fait désormais foi entre le propriétaire et le locataire.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%2$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Loyer mensuel</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%3$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Charges mensuelles</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Dépôt de garantie</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%5$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Début du bail</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%6$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Durée</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%7$s mois</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>%8$s
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="{{URL_APP}}documents" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Voir mes documents</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 08 · invitation-signature-edl ─────────────────────────────
    public static final String SUJET_MAIL_INVITATION_SIGNATURE_EDL =
            "Votre état des lieux est prêt à signer · Kupanga";

    // %1$s = prénom/nom locataire · %2$s = prénom/nom propriétaire · %3$s = adresse
    // %4$s = type EDL · %5$s = date réalisation (formatée) · %6$s = lien signature
    public static final String CONTENU_MAIL_INVITATION_SIGNATURE_EDL =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">%2$s vous invite à signer l'état des lieux.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#fbefdd; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#8a5a12;"><span style="color:#b7791f;">&#9679;</span>&nbsp;À signer</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre état des lieux est prêt à signer</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;"><strong>%2$s</strong> vous invite à signer l'état des lieux que vous avez réalisé ensemble. Relisez-le pièce par pièce avant de signer.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%3$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Type</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Réalisé le</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%5$s</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%6$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Lire et signer l'état des lieux</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
          <td class="kp-warn" style="background:#fbefdd; border-radius:12px; padding:14px 16px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:14px; line-height:20px; color:#8a5a12;">Ce lien est personnel et valable 72 heures. Passé ce délai, demandez-en un nouveau à %2$s.</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Le bouton ne fonctionne pas ? Copiez ce lien dans votre navigateur :<br><a href="%6$s" class="kp-acc" style="color:#4f6a2e; word-break:break-all;">%6$s</a></p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 09 · edl-signe ─────────────────────────────
    public static final String SUJET_MAIL_EDL_SIGNE =
            "Votre état des lieux est signé · Kupanga";

    // %1$s = prénom/nom destinataire · %2$s = adresse · %3$s = type EDL · %4$s = date réalisation (formatée)
    // %5$s = NOUVEAU : bloc « Document joint » (BLOC_PIECE_JOINTE rempli, ou chaîne vide sans PDF)
    public static final String CONTENU_MAIL_EDL_SIGNE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Votre état des lieux est signé par les deux parties.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#e5f1e8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#22663a;"><span style="color:#2f8a4e;">&#9679;</span>&nbsp;Signé</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre état des lieux est signé</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'état des lieux est signé par les deux parties.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%2$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Type</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%3$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Réalisé le</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>%5$s
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
          <td class="kp-info" style="background:#edf3e0; border-radius:12px; padding:14px 16px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:14px; line-height:20px; color:#3a5122;">Gardez-le jusqu'à la fin de la location : c'est la référence pour comparer l'état du logement à la sortie.</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="{{URL_APP}}documents" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Voir mes documents</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 10 · quittance ─────────────────────────────
    public static final String SUJET_MAIL_QUITTANCE =
            "Votre quittance de loyer est disponible · Kupanga";

    // %1$s = prénom/nom locataire · %2$s = mois + année · %3$s = adresse
    // %4$s = loyer HC · %5$s = charges · %6$s = total · %7$s = date paiement (formatée)
    // %8$s = NOUVEAU : bloc « Document joint » (BLOC_PIECE_JOINTE rempli, ou chaîne vide sans PDF)
    public static final String CONTENU_MAIL_QUITTANCE =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">Quittance de loyer, %2$s.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#e5f1e8; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#22663a;"><span style="color:#2f8a4e;">&#9679;</span>&nbsp;Quittance signée</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">Votre quittance de loyer</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Votre propriétaire a confirmé avoir reçu votre loyer. Voici la quittance correspondante.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Période</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%2$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%3$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Loyer hors charges</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Charges</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%5$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Payé le</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%7$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Total payé</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:20px; line-height:28px; font-weight:500; color:#10212b;">%6$s</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>%8$s
      <tr><td class="kp-pad" style="padding:20px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Cette quittance a valeur de reçu, conformément à l'article 21 de la loi du 6 juillet 1989.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="{{URL_APP}}documents" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Voir mes quittances</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 11 · demande-location ─────────────────────────────
    public static final String SUJET_MAIL_DEMANDE_LOCATION =
            "Un propriétaire vous propose son logement · Kupanga";

    // %1$s = prénom/nom locataire · %2$s = prénom/nom propriétaire · %3$s = adresse
    // %4$s = intitulé du logement · %5$s = date et heure d'expiration (formatées) · %6$s = lien vers les demandes reçues
    public static final String CONTENU_MAIL_DEMANDE_LOCATION =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">%2$s vous propose son logement. Réponse attendue avant le %5$s.</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:#fbefdd; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:#8a5a12;"><span style="color:#b7791f;">&#9679;</span>&nbsp;Réponse attendue</td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">%2$s vous propose son logement</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;"><strong>%2$s</strong> vous propose de devenir locataire de son logement. Il ne vous sera attribué que si vous acceptez.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Logement</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%4$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Adresse</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%3$s</div>
            </td></tr><tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">Répondre avant le</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%5$s</div>
            </td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%6$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">Voir la demande</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:20px 40px 0;">
        <p class="kp-t3" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">Sans réponse avant le %5$s, la demande expire. Vous ne connaissez pas ce propriétaire ? Ignorez cet e-mail.</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── 12 à 18 · Fin de réservation, congé, fin de bail ─────────────
    // Sujet construit par EmailServiceImpl selon le cas.
    // CHANGEMENT DE PARAMÈTRES (les 3 appels sont dans EmailServiceImpl) :
    // %1$s = prénom/nom destinataire · %2$s = badge d'état (BLOC_BADGE rempli) · %3$s = titre
    // %4$s = message (HTML, valeurs déjà échappées) · %5$s = libellé du bouton · %6$s = lien
    // %7$s = lignes du récapitulatif (LIGNE_RECAP remplies, concaténées) · %8$s = texte d'aperçu (preheader)
    public static final String CONTENU_MAIL_FIN_RESERVATION =
            """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>Kupanga</title>
<link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  body { margin:0; padding:0; }
  .kp-tab { font-variant-numeric:tabular-nums; }
  @media (max-width:480px) {
    .kp-out { padding:16px 8px !important; }
    .kp-pad { padding-left:20px !important; padding-right:20px !important; }
    .kp-h1 { font-size:22px !important; line-height:28px !important; }
    .kp-btnwrap { width:100%% !important; }
    .kp-btn a { display:block !important; text-align:center !important; }
  }
  @media (prefers-color-scheme:dark) {
    .kp-bg { background:#0b181f !important; }
    .kp-card { background:#10212b !important; border-color:#1f3644 !important; }
    .kp-t1 { color:#effbdb !important; }
    .kp-t2 { color:#c3cdbf !important; }
    .kp-t3 { color:#93a19a !important; }
    .kp-acc { color:#a1b577 !important; }
    .kp-box { background:#0b181f !important; }
    .kp-line { border-color:#1f3644 !important; }
    .kp-btn { background:#a1b577 !important; }
    .kp-btn a { color:#10212b !important; }
    .kp-info { background:#1b2d1c !important; color:#d6e3bd !important; }
    .kp-warn { background:#33270f !important; color:#f1d9a8 !important; }
    .kp-ico { background:#1b2d1c !important; color:#a1b577 !important; }
    .kp-logo-clair { display:none !important; }
    .kp-logo-sombre { display:block !important; max-height:none !important; height:32px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background:#f4f5f1;">
<div style="display:none; max-height:0; overflow:hidden; mso-hide:all; font-size:1px; line-height:1px; color:#f4f5f1;">%8$s</div>
<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-bg" style="background:#f4f5f1;">
  <tr><td align="center" class="kp-out" style="padding:32px 16px;">
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" class="kp-card" style="width:100%%; max-width:600px; background:#ffffff; border:1px solid #e2e4e6; border-radius:20px;">
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <a href="{{URL_APP}}" style="text-decoration:none;">
          <img class="kp-logo-clair" src="{{URL_APP}}email/kupanga-logo-clair.png" width="138" height="32" alt="kupanga" style="display:block; width:138px; height:32px; border:0; outline:none; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:22px; line-height:32px; font-weight:600; color:#10212b;">
          <!--[if !mso]><!--><img class="kp-logo-sombre" src="{{URL_APP}}email/kupanga-logo-sombre.png" width="138" height="32" alt="kupanga" style="display:none; max-height:0; overflow:hidden; mso-hide:all; width:138px; border:0; outline:none;"><!--<![endif]-->
        </a>
      </td></tr>
      <tr><td class="kp-pad" style="padding:32px 40px 0;">
        %2$s
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <h1 class="kp-t1 kp-h1" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:26px; line-height:32px; font-weight:500; letter-spacing:-0.01em; color:#10212b;">%3$s</h1>
      </td></tr>
      <tr><td class="kp-pad" style="padding:16px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">Bonjour %1$s,</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:12px 40px 0;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">%4$s</p>
      </td></tr>
      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-box" style="background:#edefe8; border-radius:12px;">
          <tr><td style="padding:8px 20px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
            %7$s
            </table>
          </td></tr>
        </table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 0;">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" class="kp-btnwrap"><tr>
          <td class="kp-btn" bgcolor="#4f6a2e" style="background:#4f6a2e; border-radius:999px;">
            <a href="%6$s" style="display:inline-block; padding:13px 28px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:20px; font-weight:500; color:#ffffff; text-decoration:none; border-radius:999px;">%5$s</a>
          </td>
        </tr></table>
      </td></tr>
      <tr><td class="kp-pad" style="padding:28px 40px 36px;">
        <p class="kp-t2" style="margin:0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:24px; color:#46545c;">L'équipe Kupanga</p>
      </td></tr>
    </table>
    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%; max-width:600px;">
      <tr><td class="kp-t3" align="center" style="padding:20px 24px 0; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:12px; line-height:18px; color:#636f76;">
        Kupanga · location entre particuliers, en France et en RDC<br>
        <a href="{{URL_APP}}" class="kp-t3" style="color:#636f76; text-decoration:underline;">{{DOMAINE_APP}}</a>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
            """;

    // ─── Blocs réutilisables (remplis dans EmailServiceImpl) ─────────────
    // %1$s = nom du fichier (échappé) · %2$s = nature du document (« Bail signé »…)
    public static final String BLOC_PIECE_JOINTE =
            """

      <tr><td class="kp-pad" style="padding:24px 40px 0;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" class="kp-line" style="border:1px solid #e2e4e6; border-radius:12px;">
          <tr>
            <td width="52" valign="top" style="padding:14px 0 14px 16px;">
              <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                <td class="kp-ico" align="center" valign="middle" width="36" height="44" style="width:36px; height:44px; background:#edf3e0; border-radius:6px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:11px; line-height:44px; font-weight:600; color:#4f6a2e;">PDF</td>
              </tr></table>
            </td>
            <td valign="middle" style="padding:14px 16px 14px 12px;">
              <div class="kp-t1" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b; word-break:break-all;">%1$s</div>
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; color:#636f76;">%2$s · PDF joint à cet e-mail. Disponible aussi dans Kupanga, rubrique Documents.</div>
            </td>
          </tr>
        </table>
      </td></tr>
            """;

    // %1$s = fond · %2$s = couleur du texte · %3$s = couleur du point · %4$s = libellé
    public static final String BLOC_BADGE =
            """
        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="background:%1$s; border-radius:999px; padding:2px 10px 2px 8px; font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:20px; font-weight:500; color:%2$s;"><span style="color:%3$s;">&#9679;</span>&nbsp;%4$s</td>
        </tr></table>
            """;

    // %1$s = libellé · %2$s = valeur (échappée)
    public static final String LIGNE_RECAP =
            """
            <tr><td style="padding:10px 0;">
              <div class="kp-t3" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:13px; line-height:18px; color:#636f76;">%1$s</div>
              <div class="kp-t1 kp-tab" style="font-family:Figtree,-apple-system,'Segoe UI',Roboto,Arial,sans-serif; font-size:15px; line-height:22px; font-weight:500; color:#10212b;">%2$s</div>
            </td></tr>
            """;
}
