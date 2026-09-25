"use client";

import Link from "next/link";
import { Header } from "@/shared/components";
import { Button, Card, Badge } from "@/shared/components";
import { useAuth } from "@/features/auth";
import { ROUTES } from "@/core/config";
import { useTranslation } from "@/core/i18n";

export default function HomePage() {
  const { isAuthenticated, user } = useAuth();
  const { t, interpolate, getLocalizedPath } = useTranslation();

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 flex flex-col justify-center items-center">
        <div className="text-center max-w-3xl mx-auto space-y-6">
          <Badge variant="primary" size="md">
            {t.home.badge}
          </Badge>

          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-zinc-900 dark:text-white">
            {t.home.heroTitle} <br />
            <span className="text-blue-600">{t.home.heroSubtitle}</span>
          </h1>

          <p className="text-lg text-zinc-600 dark:text-zinc-400">
            {t.home.description}
          </p>

          <div className="flex items-center justify-center gap-4 pt-4">
            {isAuthenticated ? (
              <Link href={getLocalizedPath(ROUTES.DASHBOARD)}>
                <Button size="lg" variant="primary">
                  {interpolate(t.home.accessDashboard, { name: user?.firstName || "" })}
                </Button>
              </Link>
            ) : (
              <>
                <Link href={getLocalizedPath(ROUTES.LOGIN)}>
                  <Button size="lg" variant="primary">
                    {t.home.loginBtn}
                  </Button>
                </Link>
                <Link href={getLocalizedPath(ROUTES.REGISTER)}>
                  <Button size="lg" variant="outline">
                    {t.home.registerBtn}
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-16 w-full">
          <Card title={t.home.backendCardTitle} subtitle={t.home.backendCardSubtitle}>
            <ul className="text-sm space-y-2 text-zinc-600 dark:text-zinc-400">
              {t.home.backendFeatures.map((feat, i) => (
                <li key={i}>{feat}</li>
              ))}
            </ul>
          </Card>

          <Card title={t.home.frontendCardTitle} subtitle={t.home.frontendCardSubtitle}>
            <ul className="text-sm space-y-2 text-zinc-600 dark:text-zinc-400">
              {t.home.frontendFeatures.map((feat, i) => (
                <li key={i}>{feat}</li>
              ))}
            </ul>
          </Card>

          <Card title={t.home.infraCardTitle} subtitle={t.home.infraCardSubtitle}>
            <ul className="text-sm space-y-2 text-zinc-600 dark:text-zinc-400">
              {t.home.infraFeatures.map((feat, i) => (
                <li key={i}>{feat}</li>
              ))}
            </ul>
          </Card>
        </div>
      </main>

      <footer className="border-t border-zinc-200 dark:border-zinc-800 py-6 text-center text-sm text-zinc-500">
        {t.home.footerText}
      </footer>
    </div>
  );
}
