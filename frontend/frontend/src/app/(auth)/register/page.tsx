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

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
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
    <div className="min-h-screen flex items-center justify-center bg-zinc-950 text-white px-6">
      <div className="w-full max-w-md bg-zinc-900 p-10 rounded-xl border border-zinc-800">
        <h2 className="text-2xl font-bold mb-6 text-center">Create your account</h2>

        <form className="flex flex-col gap-4" onSubmit={handleRegister}>
          <label className="text-sm text-gray-400" htmlFor="reg-first-name">
            First name
          </label>
          <input
            id="reg-first-name"
            className="p-3 rounded bg-zinc-800 border border-zinc-700"
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
            className="p-3 rounded bg-zinc-800 border border-zinc-700"
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
            className="p-3 rounded bg-zinc-800 border border-zinc-700"
            type="password"
            placeholder="Choose a secure password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            autoComplete="new-password"
            required
          />

          <label className="text-sm text-gray-400" htmlFor="reg-confirm">
            Confirm password
          </label>
          <input
            id="reg-confirm"
            className="p-3 rounded bg-zinc-800 border border-zinc-700"
            type="password"
            placeholder="Repeat your password"
            value={confirmPassword}
            onChange={event => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
            required
          />

          {error && <p className="text-sm text-red-400">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="p-3 bg-green-600 hover:bg-green-500 rounded-lg font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "Creating account..." : "Register"}
          </button>
        </form>

        <p className="text-sm text-gray-400 text-center mt-6">
          Already have an account?{" "}
          <Link href="/login" className="text-green-300 hover:underline">
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}
