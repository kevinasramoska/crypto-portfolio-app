"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getActiveProfile, getToken, logout, type ActiveProfile } from "@/lib/auth";

type Props = {
  showAuthLinks?: boolean;
};

export default function Navbar({ showAuthLinks = true }: Props) {
  const [profile, setProfile] = useState<ActiveProfile | null>(null);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!getToken()) {
      setProfile(null);
      return;
    }
    setProfile(getActiveProfile());
  }, [pathname]);

  function handleLogout() {
    logout();
    setProfile(null);
    router.push("/login");
  }

  const showGuestLinks = showAuthLinks && !profile;

  return (
    <nav className="border-b border-gray-800 bg-black/40 backdrop-blur sticky top-0 z-10">
      <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        <Link href="/dashboard" className="text-xl font-semibold tracking-wide">
          Crypto<span className="text-purple-400">Tracker</span>
        </Link>

        <div className="flex items-center gap-6 text-sm">
          <Link href="/dashboard" className="hover:text-purple-400 transition-colors">
            Dashboard
          </Link>

          {profile ? (
            <>
              <span className="text-gray-300">Hi, {profile.firstName}</span>
              <button
                onClick={handleLogout}
                className="rounded-lg border border-purple-500/60 px-4 py-1.5 text-purple-200 hover:bg-purple-600/20 transition"
              >
                Logout
              </button>
            </>
          ) : (
            showGuestLinks && (
              <>
                <Link href="/login" className="hover:text-purple-400 transition-colors">
                  Login
                </Link>
                <Link href="/register" className="hover:text-purple-400 transition-colors">
                  Register
                </Link>
              </>
            )
          )}
        </div>
      </div>
    </nav>
  );
}
