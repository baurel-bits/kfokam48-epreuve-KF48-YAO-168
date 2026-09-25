/**
 * Routes des trois écrans imposés par le cahier des charges (F2).
 * Dans le périmètre de l'issue #8, seul l'écran formateur est branché.
 */
export const ROUTES = {
  FORMATEUR: "/formateur",
  ETUDIANT: "/etudiant",
  RELECTEUR: "/relecteur",
} as const;

export type Route = (typeof ROUTES)[keyof typeof ROUTES];
