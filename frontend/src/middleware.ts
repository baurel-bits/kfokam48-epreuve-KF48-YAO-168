import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import {
  ROUTES,
  LOCALES,
  DEFAULT_LOCALE,
  isProtectedRoute,
  isAuthRoute,
  stripLocaleFromPath,
  getLocalePath,
} from "@/core/config";
import { Locale } from "@/core/i18n";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("auth_token")?.value;

  // 1. Vérifier si le chemin contient déjà un préfixe de locale valide
  const segments = pathname.split("/").filter(Boolean);
  const firstSegment = segments[0] as Locale;
  const hasLocale = LOCALES.includes(firstSegment);

  if (!hasLocale) {
    // Négociation ou utilisation de la locale par défaut
    const savedLocale = request.cookies.get("app_locale")?.value as Locale;
    const localeToUse = LOCALES.includes(savedLocale) ? savedLocale : DEFAULT_LOCALE;

    const newUrl = new URL(
      getLocalePath(pathname === "/" ? "" : pathname, localeToUse),
      request.url
    );
    newUrl.search = request.nextUrl.search;
    return NextResponse.redirect(newUrl);
  }

  const currentLocale = firstSegment;
  const { pathWithoutLocale } = stripLocaleFromPath(pathname);
  const normalizedPath = pathWithoutLocale === "" ? "/" : pathWithoutLocale;

  // 2. Redirection si utilisateur non connecté accède à une route protégée
  if (isProtectedRoute(pathname) && !token) {
    const loginUrl = new URL(getLocalePath(ROUTES.LOGIN, currentLocale), request.url);
    loginUrl.searchParams.set("redirect", pathname);
    return NextResponse.redirect(loginUrl);
  }

  // 3. Redirection si utilisateur déjà connecté tente d'accéder à /login ou /register
  if (isAuthRoute(pathname) && token) {
    return NextResponse.redirect(
      new URL(getLocalePath(ROUTES.DASHBOARD, currentLocale), request.url)
    );
  }

  const response = NextResponse.next();
  // Synchroniser la locale dans un cookie pour la persistance
  response.cookies.set("app_locale", currentLocale, {
    path: "/",
    maxAge: 31536000,
    sameSite: "lax",
  });

  return response;
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for:
     * - api routes
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico, sitemap.xml, robots.txt
     */
    "/((?!api|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
  ],
};
