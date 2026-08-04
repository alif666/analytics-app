"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function LoginPage() {
  const router = useRouter();
  const { login, isLoading } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    const result = await login(username, password);

    if (!result.success) {
      setError(result.error);
      return;
    }

    router.replace("/dashboard");
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 text-foreground">
      <div className="w-full max-w-md rounded-3xl border border-border bg-surface/90 p-8 shadow-[0_20px_80px_rgba(0,0,0,0.12)] backdrop-blur">
        <div className="mb-8">
          <p className="text-sm uppercase tracking-[0.25em] text-muted">Analytics access</p>
          <h1 className="mt-2 text-3xl font-semibold">Sign in</h1>
          <p className="mt-2 text-sm text-muted">
            Uses Spring Boot JWT plus CSRF cookie flow.
          </p>
        </div>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input
              type="email"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              className="w-full rounded-xl border border-border bg-background px-3 py-2.5 outline-none transition focus:border-accent"
              placeholder="Enter email"
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              className="w-full rounded-xl border border-border bg-background px-3 py-2.5 outline-none transition focus:border-accent"
              placeholder="Enter password"
              required
            />
          </div>

          {error ? (
            <div className="rounded-xl border border-red-400/40 bg-red-500/10 px-3 py-2 text-sm text-red-300">
              {error}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-xl bg-foreground px-4 py-2.5 font-medium text-background transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLoading ? "Signing in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
}
