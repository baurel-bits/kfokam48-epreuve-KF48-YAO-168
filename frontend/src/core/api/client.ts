import { API_BASE_URL, ENDPOINTS } from "./endpoints";
import { ApiError, ApiResponse, RequestOptions } from "./types";

const ACCESS_TOKEN_KEY = "auth_access_token";
const REFRESH_TOKEN_KEY = "auth_refresh_token";
const COOKIE_TOKEN_KEY = "auth_token";

export const tokenStorage = {
  getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },
  setAccessToken(token: string): void {
    if (typeof window !== "undefined") {
      localStorage.setItem(ACCESS_TOKEN_KEY, token);
      // Synchroniser avec un cookie pour le middleware Next.js
      document.cookie = `${COOKIE_TOKEN_KEY}=${encodeURIComponent(token)}; path=/; max-age=86400; SameSite=Lax`;
    }
  },
  getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },
  setRefreshToken(token: string): void {
    if (typeof window !== "undefined") {
      localStorage.setItem(REFRESH_TOKEN_KEY, token);
    }
  },
  clear(): void {
    if (typeof window !== "undefined") {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      // Supprimer le cookie
      document.cookie = `${COOKIE_TOKEN_KEY}=; path=/; max-age=0; SameSite=Lax`;
    }
  },
};

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

async function refreshToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    tokenStorage.clear();
    return null;
  }

  try {
    const response = await fetch(`${API_BASE_URL}${ENDPOINTS.AUTH.REFRESH}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      tokenStorage.clear();
      return null;
    }

    const data: ApiResponse<{ accessToken: string; refreshToken: string }> =
      await response.json();

    if (data.data?.accessToken) {
      tokenStorage.setAccessToken(data.data.accessToken);
      if (data.data.refreshToken) {
        tokenStorage.setRefreshToken(data.data.refreshToken);
      }
      return data.data.accessToken;
    }
    tokenStorage.clear();
    return null;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<ApiResponse<T>> {
  const { params, requiresAuth = true, headers: customHeaders, ...restOptions } =
    options;

  let url = `${API_BASE_URL}${endpoint}`;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null) {
        searchParams.append(key, String(val));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += `?${queryString}`;
    }
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...(customHeaders as Record<string, string>),
  };

  if (requiresAuth) {
    const token = tokenStorage.getAccessToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  let response = await fetch(url, {
    ...restOptions,
    headers,
  });

  // Gestion de l'expiration du token (401) et refresh automatique
  if (response.status === 401 && requiresAuth && tokenStorage.getRefreshToken()) {
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const newToken = await refreshToken();
        if (newToken) {
          processQueue(null, newToken);
          headers["Authorization"] = `Bearer ${newToken}`;
          response = await fetch(url, {
            ...restOptions,
            headers,
          });
        } else {
          processQueue(new Error("Refresh token expired"), null);
        }
      } catch (refreshErr) {
        processQueue(refreshErr, null);
      } finally {
        isRefreshing = false;
      }
    } else {
      // Si un refresh est déjà en cours, patienter
      const token = await new Promise<string | null>((resolve, reject) => {
        failedQueue.push({
          resolve: (t) => resolve(t as string),
          reject,
        });
      });
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
        response = await fetch(url, {
          ...restOptions,
          headers,
        });
      }
    }
  }

  let data: ApiResponse<T>;
  try {
    data = await response.json();
  } catch {
    throw new ApiError(
      response.statusText || "Erreur de communication avec le serveur",
      response.status
    );
  }

  if (!response.ok || !data.success) {
    throw new ApiError(
      data.message || "Une erreur est survenue",
      response.status,
      data.errors
    );
  }

  return data;
}

export const http = {
  get<T>(endpoint: string, options?: Omit<RequestOptions, "method" | "body">) {
    return apiClient<T>(endpoint, { ...options, method: "GET" });
  },
  post<T>(
    endpoint: string,
    body?: unknown,
    options?: Omit<RequestOptions, "method" | "body">
  ) {
    return apiClient<T>(endpoint, {
      ...options,
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    });
  },
  put<T>(
    endpoint: string,
    body?: unknown,
    options?: Omit<RequestOptions, "method" | "body">
  ) {
    return apiClient<T>(endpoint, {
      ...options,
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    });
  },
  patch<T>(
    endpoint: string,
    body?: unknown,
    options?: Omit<RequestOptions, "method" | "body">
  ) {
    return apiClient<T>(endpoint, {
      ...options,
      method: "PATCH",
      body: body ? JSON.stringify(body) : undefined,
    });
  },
  delete<T>(
    endpoint: string,
    options?: Omit<RequestOptions, "method" | "body">
  ) {
    return apiClient<T>(endpoint, { ...options, method: "DELETE" });
  },
};
