"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getActiveProfile, getToken, logout, subscribeAuth, type ActiveProfile } from "@/lib/auth";

type Props = {
  showAuthLinks?: boolean;
};

export default function Navbar({ showAuthLinks = true }: Props) {
  const [profile, setProfile] = useState<ActiveProfile | null>(null);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const unsubscribe = subscribeAuth(() => setProfile(getToken() ? getActiveProfile() : null));
    queueMicrotask(() => {
      if (!getToken()) {
        setProfile(null);
        return;
      }
      setProfile(getActiveProfile());
    });
    return unsubscribe;
  }, [pathname]);

  function handleLogout() {
    logout();
    setProfile(null);
    router.push("/login");
  }

  const showGuestLinks = showAuthLinks && !profile;
  const dashboardIsActive = pathname === "/dashboard";

  return (
    <nav className="sticky top-0 z-10 border-b border-gray-800/80 bg-black/65 backdrop-blur-md">
      <div className="mx-auto flex min-h-16 max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link href="/" className="rounded-md text-lg font-semibold tracking-wide transition hover:text-purple-200 sm:text-xl">
          Crypto<span className="text-purple-400">Tracker</span>
        </Link>

        <div className="flex flex-wrap items-center gap-2 text-sm sm:gap-4">
          <Link href="/demo" aria-current={pathname === "/demo" ? "page" : undefined} className="rounded-md px-2 py-1.5 text-purple-200 hover:bg-purple-500/10">Demo</Link>
          <Link href="/engineering" aria-current={pathname === "/engineering" ? "page" : undefined} className="rounded-md px-2 py-1.5 text-gray-300 hover:text-purple-200">Engineering</Link>
          <Link
            href="/dashboard"
            aria-current={dashboardIsActive ? "page" : undefined}
            className={`rounded-md px-2 py-1.5 transition hover:bg-purple-500/10 hover:text-purple-300 ${
              dashboardIsActive ? "bg-purple-500/10 text-purple-200" : "text-gray-300"
            }`}
          >
            Dashboard
          </Link>

          {profile ? (
            <>
              <span className="hidden text-gray-300 sm:inline">Hi, {profile.firstName}</span>
              <button
                onClick={handleLogout}
                className="rounded-lg border border-purple-500/60 px-3 py-1.5 text-purple-100 transition hover:bg-purple-600/20 sm:px-4"
              >
                Logout
              </button>
            </>
          ) : (
            showGuestLinks && (
              <>
                <Link href="/login" className="rounded-md px-2 py-1.5 transition hover:bg-purple-500/10 hover:text-purple-300">
                  Log in
                </Link>
                <Link href="/register" className="rounded-lg bg-purple-600 px-3 py-1.5 font-medium text-white transition hover:bg-purple-500 sm:px-4">
                  Get started
                </Link>
              </>
            )
          )}
        </div>
      </div>
    </nav>
  );
}
