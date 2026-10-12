"use client";

import { useState, type FormEvent } from "react";
import { useUser } from "@/shared/hooks/useUser";
import { api, toError } from "@/shared/lib/api";

export function LoginScreen() {
  const { login } = useUser();
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const normalized = username.trim();
    if (normalized.length < 2 || normalized.length > 40) {
      setError("Use between 2 and 40 characters.");
      return;
    }
    setSubmitting(true);
    try {
      login(await api.login(normalized));
    } catch (reason) {
      setError(toError(reason, "Login is unavailable. Please try again.").message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen place-items-center bg-[#f5f7f3] px-5 py-12">
      <section className="w-full max-w-md rounded-3xl bg-white p-8 shadow-[0_24px_80px_rgba(15,23,42,0.10)] sm:p-10" aria-labelledby="login-title">
        <p className="text-3xl font-bold tracking-tight text-slate-700">
          <span className="text-emerald-600">arc</span>ade<span className="text-emerald-500">.</span>
        </p>
        <h1 id="login-title" className="mt-10 text-2xl font-bold tracking-tight text-slate-900">
          Welcome
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">Choose a username to enter the local store. No password is required for this project demo.</p>
        <form onSubmit={(event) => void submit(event)} className="mt-7 space-y-4" noValidate>
          <div>
            <label htmlFor="username" className="label">
              Username
            </label>
            <input
              id="username"
              name="username"
              className="input h-12"
              autoComplete="username"
              maxLength={40}
              autoFocus
              value={username}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? "username-error" : undefined}
              onChange={(event) => {
                setUsername(event.target.value);
                setError(undefined);
              }}
            />
            {error && (
              <p id="username-error" role="alert" className="field-error">
                {error}
              </p>
            )}
          </div>
          <button type="submit" className="btn btn-primary h-12 w-full" disabled={!username.trim() || submitting}>
            {submitting ? "Signing in…" : "Continue"}
          </button>
        </form>
        <p className="mt-6 text-xs leading-5 text-slate-400">The backend creates a private UUID for your username. It is stored in this browser and never shown in the storefront.</p>
      </section>
    </main>
  );
}
