import { Locale } from "../i18n/translations";

export const LOCALES: Locale[] = ["fr", "en"];
export const DEFAULT_LOCALE: Locale = "fr";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  DASHBOARD: "/dashboard",
} as const;

export const AUTH_ROUTES: string[] = [
  ROUTES.LOGIN,
  ROUTES.REGISTER,
];

export const PUBLIC_ROUTES: string[] = [
  ROUTES.HOME,
  ...AUTH_ROUTES,
];

export const PROTECTED_ROUTES: string[] = [
  ROUTES.DASHBOARD,
];

/**
 * Retire le segment de locale éventuel d'un pathname (/fr/dashboard -> /dashboard)
 */
export function stripLocaleFromPath(pathname: string): {
  locale: Locale;
  pathWithoutLocale: string;
} {
  const segments = pathname.split("/").filter(Boolean);
  const firstSegment = segments[0] as Locale;

  if (LOCALES.includes(firstSegment)) {
    const pathWithoutLocale = "/" + segments.slice(1).join("/");
    return {
      locale: firstSegment,
      pathWithoutLocale: pathWithoutLocale === "/" ? "" : pathWithoutLocale,
    };
  }

  return {
    locale: DEFAULT_LOCALE,
    pathWithoutLocale: pathname === "/" ? "" : pathname,
  };
}

/**
 * Construit un chemin préfixé par la locale (ex: getLocalePath('/dashboard', 'en') -> '/en/dashboard')
 */
export function getLocalePath(path: string, locale: Locale): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  if (normalizedPath === "/") {
    return `/${locale}`;
  }
  return `/${locale}${normalizedPath}`;
}

export function isProtectedRoute(pathname: string): boolean {
  const { pathWithoutLocale } = stripLocaleFromPath(pathname);
  const path = pathWithoutLocale === "" ? "/" : pathWithoutLocale;
  return PROTECTED_ROUTES.some(
    (route) => path === route || path.startsWith(`${route}/`)
  );
}

export function isAuthRoute(pathname: string): boolean {
  const { pathWithoutLocale } = stripLocaleFromPath(pathname);
  const path = pathWithoutLocale === "" ? "/" : pathWithoutLocale;
  return AUTH_ROUTES.includes(path);
}
