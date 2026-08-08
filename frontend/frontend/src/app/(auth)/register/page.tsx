"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { register } from "@/lib/api";
import { saveToken, setActiveProfile } from "@/lib/auth";

export default function RegisterPage() {
  const [firstName, setFirstName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!firstName.trim()) {
      setError("First name is required.");
      return;
    }

    if (!email.trim()) {
      setError("Email is required.");
      return;
    }

    if (!password) {
      setError("Password is required.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setError(null);

    try {
      setLoading(true);
      const trimmedEmail = email.trim();
      const trimmedFirstName = firstName.trim();
      const res = await register(trimmedEmail, password);
      saveToken(res.accessToken);
      setActiveProfile(trimmedEmail, trimmedFirstName);
      window.location.href = "/dashboard";
    } catch {
      setError("Registration failed. Try a different email or try again later.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-[calc(100vh-8.5rem)] items-center justify-center px-4 py-12 sm:px-6">
      <div className="w-full max-w-md rounded-2xl border border-gray-800 bg-gray-950/70 p-6 shadow-2xl shadow-purple-950/20 sm:p-10">
        <p className="text-center text-xs font-semibold uppercase tracking-[0.2em] text-purple-300">Start tracking</p>
        <h1 className="mt-3 text-center text-3xl font-bold tracking-tight">Create your account</h1>
        <p className="mt-3 text-center text-sm leading-6 text-gray-400">Add your first buy, then follow your holdings and portfolio performance.</p>

        <form className="mt-8 flex flex-col gap-5" onSubmit={handleRegister} noValidate>
          <label className="text-sm text-gray-400" htmlFor="reg-first-name">
            First name
          </label>
          <input
            id="reg-first-name"
            className="mt-2 rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            type="text"
            placeholder="Satoshi"
            value={firstName}
            onChange={event => setFirstName(event.target.value)}
            autoComplete="given-name"
            required
          />
          <label className="text-sm text-gray-400" htmlFor="reg-email">
            Email
          </label>
          <input
            id="reg-email"
            className="mt-2 rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={event => setEmail(event.target.value)}
            autoComplete="email"
            required
          />

          <label className="text-sm text-gray-400" htmlFor="reg-password">
            Password
          </label>
          <input
            id="reg-password"
            className="mt-2 rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            type="password"
            placeholder="Choose a secure password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
          />
          <p className="-mt-3 text-xs text-gray-500">Use at least 8 characters.</p>

          <label className="text-sm text-gray-400" htmlFor="reg-confirm">
            Confirm password
          </label>
          <input
            id="reg-confirm"
            className="mt-2 rounded-lg border border-gray-800 bg-gray-900/70 px-4 py-3 text-white placeholder:text-gray-500 focus:border-purple-400 focus:outline-hidden"
            type="password"
            placeholder="Repeat your password"
            value={confirmPassword}
            onChange={event => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
          />

          {error && <p className="text-sm text-red-300" role="alert">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-purple-600 px-4 py-3 font-semibold text-white transition hover:bg-purple-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? "Creating account..." : "Register"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-400">
          Already have an account?{" "}
          <Link href="/login" className="text-purple-300 hover:text-white hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </main>
  );
}
