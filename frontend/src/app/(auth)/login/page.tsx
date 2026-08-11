"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { login } from "@/lib/api";
import { saveToken, setActiveProfile } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail || !password) {
      setError("Email and password are required.");
      return;
    }
    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    try {
      setLoading(true);
      const res = await login(trimmedEmail, password);
      saveToken(res.accessToken);
      setActiveProfile(trimmedEmail);
      router.push("/dashboard");
    } catch {
      setError("Invalid credentials. Please check your email and password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-[calc(100vh-8.5rem)] items-center justify-center px-4 py-12 sm:px-6">
      <div className="w-full max-w-md rounded-2xl border border-gray-800 bg-gray-950/70 p-6 shadow-2xl shadow-purple-950/20 sm:p-10">
        <p className="text-center text-xs font-semibold uppercase tracking-[0.2em] text-purple-300">Welcome back</p>
        <h1 className="mt-3 text-center text-3xl font-bold tracking-tight">Log in to your portfolio</h1>
        <p className="mt-3 text-center text-sm leading-6 text-gray-400">Pick up where you left off with your saved holdings and transactions.</p>

        <form className="mt-8 space-y-5" onSubmit={handleLogin} noValidate>
          <label className="block text-sm text-gray-400" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            className="mt-2 w-full rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            placeholder="you@example.com"
            type="email"
            autoComplete="email"
            value={email}
            onChange={event => setEmail(event.target.value)}
            required
          />

          <label className="block text-sm text-gray-400" htmlFor="password">
            Password
          </label>
          <input
            id="password"
            className="mt-2 w-full rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            placeholder="••••••••"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            required
            minLength={8}
            maxLength={72}
          />

          {error && <p className="text-sm text-red-300" role="alert">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-purple-600 px-4 py-3 font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-400">
          Need an account?{" "}
          <Link href="/register" className="text-purple-300 hover:text-white hover:underline">
            Register
          </Link>
        </p>
      </div>
    </main>
  );
}
