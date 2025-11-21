"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { login } from "@/lib/api";
import { saveToken, setActiveProfile } from "@/lib/auth";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    try {
      setLoading(true);
      const trimmedEmail = email.trim();
      const res = await login(trimmedEmail, password);
      saveToken(res.accessToken);
      setActiveProfile(trimmedEmail);
      window.location.href = "/dashboard";
    } catch {
      setError("Invalid credentials. Please check your email and password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-950 text-white px-6">
      <div className="bg-zinc-900 p-10 rounded-xl w-full max-w-md border border-zinc-800">
        <h1 className="text-2xl font-bold mb-6 text-center">Login</h1>

        <form className="space-y-4" onSubmit={handleLogin}>
          <label className="block text-sm text-gray-400" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            className="w-full p-3 bg-zinc-800 rounded border border-zinc-700"
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
            className="w-full p-3 bg-zinc-800 rounded border border-zinc-700"
            placeholder="••••••••"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            required
          />

          {error && <p className="text-sm text-red-400">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full p-3 bg-indigo-600 rounded-lg font-semibold hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <p className="text-sm text-gray-400 text-center mt-6">
          Need an account?{" "}
          <Link href="/register" className="text-indigo-400 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
