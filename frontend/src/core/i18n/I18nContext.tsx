"use client";

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import { usePathname, useRouter } from "next/navigation";
import { Locale, translations, TranslationSchema } from "./translations";
import { DEFAULT_LOCALE, LOCALES, stripLocaleFromPath } from "../config/routes";

interface I18nContextType {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: TranslationSchema;
  interpolate: (text: string, params: Record<string, string | number>) => string;
  getLocalizedPath: (path: string) => string;
}

const I18N_STORAGE_KEY = "app_locale";

const I18nContext = createContext<I18nContextType | undefined>(undefined);

export function I18nProvider({
  children,
  initialLocale,
}: {
  children: React.ReactNode;
  initialLocale?: Locale;
}) {
  const pathname = usePathname();
  const router = useRouter();

  const [locale, setLocaleState] = useState<Locale>(() => {
    if (initialLocale && LOCALES.includes(initialLocale)) {
      return initialLocale;
    }
    const { locale: pathLocale } = stripLocaleFromPath(pathname || "/");
    return pathLocale || DEFAULT_LOCALE;
  });

  useEffect(() => {
    const { locale: pathLocale } = stripLocaleFromPath(pathname || "/");
    if (pathLocale && pathLocale !== locale) {
      setLocaleState(pathLocale);
    }
  }, [pathname, locale]);

  const setLocale = useCallback(
    (newLocale: Locale) => {
      if (newLocale === locale) return;
      setLocaleState(newLocale);
      localStorage.setItem(I18N_STORAGE_KEY, newLocale);

      const { pathWithoutLocale } = stripLocaleFromPath(pathname || "/");
      const targetPath =
        pathWithoutLocale === "" ? `/${newLocale}` : `/${newLocale}${pathWithoutLocale}`;
      router.push(targetPath);
    },
    [locale, pathname, router]
  );

  const getLocalizedPath = useCallback(
    (path: string): string => {
      const normalized = path.startsWith("/") ? path : `/${path}`;
      if (normalized === "/") {
        return `/${locale}`;
      }
      return `/${locale}${normalized}`;
    },
    [locale]
  );

  const interpolate = useCallback(
    (text: string, params: Record<string, string | number>): string => {
      let result = text;
      Object.entries(params).forEach(([key, val]) => {
        result = result.replace(new RegExp(`\\{${key}\\}`, "g"), String(val));
      });
      return result;
    },
    []
  );

  return (
    <I18nContext.Provider
      value={{
        locale,
        setLocale,
        t: translations[locale] || translations[DEFAULT_LOCALE],
        interpolate,
        getLocalizedPath,
      }}
    >
      {children}
    </I18nContext.Provider>
  );
}

export function useTranslation(): I18nContextType {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useTranslation doit être utilisé au sein d'un I18nProvider");
  }
  return context;
}
