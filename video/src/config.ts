/** Réglages de la vidéo à adapter avant le rendu final. */
export const VIDEO_CONFIG = {
  /** Adresse affichée à la fin ; vide = non affichée. */
  siteUrl: 'kupanga.lespacelibellule.com',
  /** Musique de fond (fichier dans public/), jouée en boucle quelle que soit la durée ; null = pas de musique. */
  music: 'music.mp3' as string | null,
  /** Volume de la musique sous la voix (0 à 1). */
  musicVolume: 0.1,
  /** Logo (fichier dans public/brand/). */
  logo: 'brand/kupanga-mark-a.svg',
  /** Légende (description de la capture) affichée en bas de chaque plan. */
  legendes: true,
};
