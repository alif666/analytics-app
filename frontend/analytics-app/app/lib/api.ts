import axios from "axios";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api";

export const LOGIN_PATH = "/auth/login";
export const LOGOUT_PATH = "/auth/logout";
export const CSRF_PATH = "/csrf-token";

export const AUTH_TOKEN_KEY = "authToken";
export const AUTH_USER_KEY = "analyticsUser";
export const CSRF_COOKIE_KEY = "XSRF-TOKEN";
export const CSRF_HEADER_KEY = "X-XSRF-TOKEN";

export const publicEndpoints = [LOGIN_PATH, CSRF_PATH];

export const isSafeMethod = (method?: string) => {
  const normalized = (method || "GET").toUpperCase();
  return ["GET", "HEAD", "OPTIONS"].includes(normalized);
};

export const isPublicEndpoint = (url?: string) => {
  if (!url) return false;
  return publicEndpoints.some(
    (endpoint) =>
      url === endpoint || url.startsWith(`${endpoint}/`) || url.startsWith(`${endpoint}?`)
  );
};

export const getCookieValue = (key: string) => {
  if (typeof document === "undefined") return undefined;

  const escapedKey = key.replace(/([.*+?^${}()|[\]\\])/g, "\\$1");
  const match = document.cookie.match(new RegExp(`(?:^|; )${escapedKey}=([^;]*)`));

  return match ? decodeURIComponent(match[1]) : undefined;
};

export async function ensureCsrfCookie() {
  let csrfToken = getCookieValue(CSRF_COOKIE_KEY);

  if (csrfToken) {
    return csrfToken;
  }

  await axios.get(`${API_BASE_URL}${CSRF_PATH}`, {
    withCredentials: true,
  });

  csrfToken = getCookieValue(CSRF_COOKIE_KEY);

  if (!csrfToken) {
    throw new Error("Unable to retrieve CSRF token cookie");
  }

  return csrfToken;
}

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(async (config) => {
  config.headers = config.headers ?? {};
  config.headers.Accept = "application/json";

  if (!isPublicEndpoint(config.url)) {
    const token = typeof window !== "undefined" ? localStorage.getItem(AUTH_TOKEN_KEY) : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  if (!isSafeMethod(config.method)) {
    const csrfToken = await ensureCsrfCookie();
    config.headers[CSRF_HEADER_KEY] = csrfToken;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && typeof window !== "undefined") {
      const isLoginRequest = error.config?.url?.includes(LOGIN_PATH);
      if (!isLoginRequest) {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem(AUTH_USER_KEY);
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export default api;
