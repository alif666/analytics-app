"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import api, { AUTH_TOKEN_KEY, AUTH_USER_KEY, LOGIN_PATH } from "@/lib/api";

type BackendUser = {
  userId?: number;
  id?: number;
  name?: string;
  email?: string;
  mobileNumber?: string;
  role?: string;
};

type AuthUser = BackendUser & {
  phone?: string;
};

type LoginResult =
  | { success: true; user: AuthUser; message?: string }
  | { success: false; error: string };

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<{ user: AuthUser | null; token: string | null }>(() => {
    if (typeof window === "undefined") {
      return { user: null, token: null };
    }

    const savedUser = localStorage.getItem(AUTH_USER_KEY);
    const savedToken = localStorage.getItem(AUTH_TOKEN_KEY);

    try {
      return {
        user: savedUser ? JSON.parse(savedUser) : null,
        token: savedToken,
      };
    } catch {
      localStorage.removeItem(AUTH_USER_KEY);
      localStorage.removeItem(AUTH_TOKEN_KEY);
      return { user: null, token: null };
    }
  });

  useEffect(() => {
    if (auth.user) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(auth.user));
    } else {
      localStorage.removeItem(AUTH_USER_KEY);
    }
  }, [auth.user]);

  useEffect(() => {
    if (auth.token) {
      localStorage.setItem(AUTH_TOKEN_KEY, auth.token);
    } else {
      localStorage.removeItem(AUTH_TOKEN_KEY);
    }
  }, [auth.token]);

  const login = async (username: string, password: string): Promise<LoginResult> => {
    try {
      const response = await api.post(LOGIN_PATH, { username, password });
      const jwtToken = response.data?.jwtToken;
      const backendUser: BackendUser = response.data?.user;

      if (!jwtToken || !backendUser) {
        return { success: false, error: "Invalid login response from server" };
      }

      const nextUser: AuthUser = {
        ...backendUser,
        phone: backendUser.mobileNumber,
      };

      setAuth({ user: nextUser, token: jwtToken });

      return {
        success: true,
        user: nextUser,
        message: response.data?.message,
      };
    } catch (error: unknown) {
      const err = error as {
        response?: { data?: { message?: string; error?: string } };
        message?: string;
      };
      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "Login failed";
      return { success: false, error: message };
    }
  };

  const logout = async () => {
    try {
      await api.post("/auth/logout");
    } catch {
      // Backend may invalidate token even if logout call fails.
    } finally {
      setAuth({ user: null, token: null });
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user: auth.user,
      token: auth.token,
      isLoading: false,
      isAuthenticated: Boolean(auth.user && auth.token),
      isAdmin: auth.user?.role === "ROLE_ADMIN",
      login,
      logout,
    }),
    [auth.user, auth.token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
}
