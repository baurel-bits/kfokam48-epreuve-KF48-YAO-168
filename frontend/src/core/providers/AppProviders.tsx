"use client";

import React from "react";
import { AuthProvider } from "@/features/auth";
import { ToastProvider } from "@/shared/components/Toast";
import { I18nProvider, Locale } from "@/core/i18n";

export function AppProviders({
  children,
  initialLocale,
}: {
  children: React.ReactNode;
  initialLocale?: Locale;
}) {
  return (
    <I18nProvider initialLocale={initialLocale}>
      <ToastProvider>
        <AuthProvider>{children}</AuthProvider>
      </ToastProvider>
    </I18nProvider>
  );
}
