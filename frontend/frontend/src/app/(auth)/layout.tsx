import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Access your account",
  description: "Create a CryptoTracker account or log in to manage your portfolio.",
};

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return children;
}
