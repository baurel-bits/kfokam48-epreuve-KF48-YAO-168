export type Locale = "fr" | "en";

export interface TranslationSchema {
  common: {
    appName: string;
    loading: string;
    error: string;
    retry: string;
    empty: string;
    success: string;
    actions: string;
    close: string;
  };
  nav: {
    home: string;
    dashboard: string;
    login: string;
    register: string;
    logout: string;
  };
  auth: {
    loginTitle: string;
    loginSubtitle: string;
    registerTitle: string;
    registerSubtitle: string;
    emailLabel: string;
    emailPlaceholder: string;
    passwordLabel: string;
    passwordPlaceholder: string;
    firstNameLabel: string;
    firstNamePlaceholder: string;
    lastNameLabel: string;
    lastNamePlaceholder: string;
    passwordHelper: string;
    submitLogin: string;
    submitRegister: string;
    noAccount: string;
    hasAccount: string;
    createAccountLink: string;
    loginLink: string;
    loginSuccess: string;
    registerSuccess: string;
    loginError: string;
    registerError: string;
  };
  dashboard: {
    greeting: string;
    welcomeSubtitle: string;
    roleBadge: string;
    userProfileTitle: string;
    userProfileSubtitle: string;
    userId: string;
    email: string;
    springRole: string;
    createdAt: string;
    cleanArchitectureTitle: string;
    cleanArchitectureSubtitle: string;
    cleanArchitectureDesc: string;
    domainStep: string;
    dataStep: string;
    presentationStep: string;
    systemStatusTitle: string;
    systemStatusSubtitle: string;
    postgresStatus: string;
    active: string;
    apiStatus: string;
    connected: string;
    swaggerUi: string;
    openLink: string;
  };
  home: {
    badge: string;
    heroTitle: string;
    heroSubtitle: string;
    description: string;
    accessDashboard: string;
    loginBtn: string;
    registerBtn: string;
    backendCardTitle: string;
    backendCardSubtitle: string;
    backendFeatures: string[];
    frontendCardTitle: string;
    frontendCardSubtitle: string;
    frontendFeatures: string[];
    infraCardTitle: string;
    infraCardSubtitle: string;
    infraFeatures: string[];
    footerText: string;
  };
  notifications: {
    title: string;
    unread: string;
    unreads: string;
    markAllAsRead: string;
    markAsRead: string;
    emptyTitle: string;
    emptyDescription: string;
    loadError: string;
  };
}

export const translations: Record<Locale, TranslationSchema> = {
  fr: {
    common: {
      appName: "Fullstack Exam",
      loading: "Chargement...",
      error: "Une erreur est survenue",
      retry: "Réessayer",
      empty: "Aucune donnée disponible",
      success: "Succès",
      actions: "Actions",
      close: "Fermer",
    },
    nav: {
      home: "Accueil",
      dashboard: "Tableau de bord",
      login: "Connexion",
      register: "Inscription",
      logout: "Déconnexion",
    },
    auth: {
      loginTitle: "Connexion",
      loginSubtitle: "Accédez à votre espace sécurisé",
      registerTitle: "Inscription",
      registerSubtitle: "Créez votre compte pour commencer",
      emailLabel: "Adresse email",
      emailPlaceholder: "nom@exemple.com",
      passwordLabel: "Mot de passe",
      passwordPlaceholder: "••••••••",
      firstNameLabel: "Prénom",
      firstNamePlaceholder: "Jean",
      lastNameLabel: "Nom",
      lastNamePlaceholder: "Dupont",
      passwordHelper: "Mot de passe (min. 6 caractères)",
      submitLogin: "Se connecter",
      submitRegister: "S'inscrire",
      noAccount: "Pas encore de compte ?",
      hasAccount: "Déjà un compte ?",
      createAccountLink: "Créer un compte",
      loginLink: "Se connecter",
      loginSuccess: "Connexion réussie !",
      registerSuccess: "Compte créé avec succès !",
      loginError: "Erreur lors de la connexion",
      registerError: "Erreur lors de l'inscription",
    },
    dashboard: {
      greeting: "Bonjour, {name} ! 👋",
      welcomeSubtitle: "Bienvenue sur votre tableau de bord. La session est authentifiée par JWT.",
      roleBadge: "Rôle : {role}",
      userProfileTitle: "Profil Utilisateur",
      userProfileSubtitle: "Informations de session",
      userId: "ID Utilisateur",
      email: "Email",
      springRole: "Rôle Spring Security",
      createdAt: "Créé le",
      cleanArchitectureTitle: "Architecture Clean Feature",
      cleanArchitectureSubtitle: "Dès réception du sujet",
      cleanArchitectureDesc: "Découpage en couches domain, data, presentation :",
      domainStep: "domain : Modèles métier et contrats d'interface (models.ts, repositories.ts).",
      dataStep: "data : Implémentations d'accès à l'API (RepositoryImpl avec apiClient).",
      presentationStep: "presentation : Composants React, hooks et contextes d'affichage.",
      systemStatusTitle: "Statut Système",
      systemStatusSubtitle: "Connectivité & Services",
      postgresStatus: "PostgreSQL",
      active: "Actif",
      apiStatus: "API Backend",
      connected: "Connecté (8080)",
      swaggerUi: "Swagger UI",
      openLink: "Ouvrir ↗",
    },
    home: {
      badge: "Architecture Clean Feature-Driven",
      heroTitle: "Socle Technique & Infrastructure",
      heroSubtitle: "Spring Boot + Next.js + PostgreSQL",
      description: "Architecture modulaire avec séparation stricte core/config/routes et features découpées en domain, data et presentation.",
      accessDashboard: "Accéder au Tableau de bord ({name})",
      loginBtn: "Se Connecter",
      registerBtn: "Créer un Compte",
      backendCardTitle: "Clean Architecture Backend",
      backendCardSubtitle: "Spring Boot 3 + Java 21",
      backendFeatures: [
        "• Découpage Domain / Application / Infrastructure",
        "• JWT Access & Refresh Token",
        "• GlobalExceptionHandler standardisé",
        "• Documentation OpenAPI / Swagger UI",
      ],
      frontendCardTitle: "Clean Feature Frontend",
      frontendCardSubtitle: "Domain / Data / Presentation",
      frontendFeatures: [
        "• core/config/routes centralisé",
        "• domain : modèles et contrats abstraits",
        "• data : implémentation API repositories",
        "• presentation : composants UI, hooks & state",
      ],
      infraCardTitle: "Infrastructure & DB",
      infraCardSubtitle: "Docker Compose + Postgres",
      infraFeatures: [
        "• PostgreSQL 17 conteneurisé",
        "• Variables d'environnement configurables",
        "• Service Mail abstrait & Notifications",
        "• Prêt pour ajouter vos features métier",
      ],
      footerText: "Template Fullstack Professionnel — Clean Architecture & Feature-Driven",
    },
    notifications: {
      title: "Notifications",
      unread: "non lue",
      unreads: "non lues",
      markAllAsRead: "Tout marquer comme lu",
      markAsRead: "Marquer lu",
      emptyTitle: "Aucune notification",
      emptyDescription: "Vous êtes à jour, rien à signaler.",
      loadError: "Impossible de charger les notifications",
    },
  },
  en: {
    common: {
      appName: "Fullstack Exam",
      loading: "Loading...",
      error: "An error occurred",
      retry: "Retry",
      empty: "No data available",
      success: "Success",
      actions: "Actions",
      close: "Close",
    },
    nav: {
      home: "Home",
      dashboard: "Dashboard",
      login: "Login",
      register: "Sign Up",
      logout: "Logout",
    },
    auth: {
      loginTitle: "Login",
      loginSubtitle: "Sign in to your secure account",
      registerTitle: "Sign Up",
      registerSubtitle: "Create your account to get started",
      emailLabel: "Email address",
      emailPlaceholder: "name@example.com",
      passwordLabel: "Password",
      passwordPlaceholder: "••••••••",
      firstNameLabel: "First name",
      firstNamePlaceholder: "John",
      lastNameLabel: "Last name",
      lastNamePlaceholder: "Doe",
      passwordHelper: "Password (min. 6 characters)",
      submitLogin: "Sign In",
      submitRegister: "Register",
      noAccount: "Don't have an account yet?",
      hasAccount: "Already have an account?",
      createAccountLink: "Create an account",
      loginLink: "Sign in",
      loginSuccess: "Signed in successfully!",
      registerSuccess: "Account created successfully!",
      loginError: "Failed to sign in",
      registerError: "Failed to register",
    },
    dashboard: {
      greeting: "Hello, {name}! 👋",
      welcomeSubtitle: "Welcome to your dashboard. The session is authenticated via JWT.",
      roleBadge: "Role: {role}",
      userProfileTitle: "User Profile",
      userProfileSubtitle: "Session information",
      userId: "User ID",
      email: "Email",
      springRole: "Spring Security Role",
      createdAt: "Created at",
      cleanArchitectureTitle: "Clean Feature Architecture",
      cleanArchitectureSubtitle: "Ready for the exam prompt",
      cleanArchitectureDesc: "Layered into domain, data, presentation:",
      domainStep: "domain: Business models and interfaces (models.ts, repositories.ts).",
      dataStep: "data: API access implementations (RepositoryImpl with apiClient).",
      presentationStep: "presentation: React UI components, custom hooks and context.",
      systemStatusTitle: "System Status",
      systemStatusSubtitle: "Connectivity & Services",
      postgresStatus: "PostgreSQL",
      active: "Active",
      apiStatus: "Backend API",
      connected: "Connected (8080)",
      swaggerUi: "Swagger UI",
      openLink: "Open ↗",
    },
    home: {
      badge: "Clean Feature-Driven Architecture",
      heroTitle: "Technical Foundation & Infrastructure",
      heroSubtitle: "Spring Boot + Next.js + PostgreSQL",
      description: "Modular architecture with strict separation of core/config/routes and features organized into domain, data, and presentation.",
      accessDashboard: "Access Dashboard ({name})",
      loginBtn: "Sign In",
      registerBtn: "Create Account",
      backendCardTitle: "Clean Architecture Backend",
      backendCardSubtitle: "Spring Boot 3 + Java 21",
      backendFeatures: [
        "• Layered Domain / Application / Infrastructure",
        "• JWT Access & Refresh Token",
        "• Standardized GlobalExceptionHandler",
        "• OpenAPI / Swagger UI Documentation",
      ],
      frontendCardTitle: "Clean Feature Frontend",
      frontendCardSubtitle: "Domain / Data / Presentation",
      frontendFeatures: [
        "• Centralized core/config/routes",
        "• domain: abstract models & contracts",
        "• data: API repository implementations",
        "• presentation: UI components, hooks & state",
      ],
      infraCardTitle: "Infrastructure & DB",
      infraCardSubtitle: "Docker Compose + Postgres",
      infraFeatures: [
        "• Containerized PostgreSQL 17",
        "• Configurable environment variables",
        "• Abstract Email Service & Notifications",
        "• Ready for domain features",
      ],
      footerText: "Professional Fullstack Template — Clean Architecture & Feature-Driven",
    },
    notifications: {
      title: "Notifications",
      unread: "unread",
      unreads: "unread",
      markAllAsRead: "Mark all as read",
      markAsRead: "Mark read",
      emptyTitle: "No notifications",
      emptyDescription: "You're all caught up.",
      loadError: "Failed to load notifications",
    },
  },
};
