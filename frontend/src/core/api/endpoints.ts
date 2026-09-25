export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const ENDPOINTS = {
  AUTH: {
    REGISTER: "/api/v1/auth/register",
    LOGIN: "/api/v1/auth/login",
    REFRESH: "/api/v1/auth/refresh",
    LOGOUT: "/api/v1/auth/logout",
    ME: "/api/v1/auth/me",
  },
  NOTIFICATIONS: {
    BASE: "/api/v1/notifications",
    UNREAD_COUNT: "/api/v1/notifications/unread-count",
    MARK_AS_READ: (id: number) => `/api/v1/notifications/${id}/read`,
    MARK_ALL_AS_READ: "/api/v1/notifications/read-all",
  },
} as const;
