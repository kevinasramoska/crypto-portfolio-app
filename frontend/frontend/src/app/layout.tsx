import "./globals.css";
import type { Metadata } from "next";
import Link from "next/link";
import Navbar from "@/components/Navbar";

export const metadata: Metadata = {
  title: {
    default: "CryptoTracker | Crypto portfolio tracking",
    template: "%s | CryptoTracker",
  },
  description: "Track crypto transactions, holdings, and realised and unrealised profit and loss.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="bg-[#0d0d0d] text-gray-100">
      <body className="min-h-screen">
        <Navbar />
        {children}
        <footer className="border-t border-gray-800/80 px-6 py-8 text-center text-xs text-gray-500">
          <p>CryptoTracker is for personal portfolio tracking and is not financial, tax, or investment advice.</p>
          <Link href="/privacy" className="mt-2 inline-block text-purple-300 transition hover:text-white">
            Privacy
          </Link>
        </footer>
      </body>
    </html>
  );
}
