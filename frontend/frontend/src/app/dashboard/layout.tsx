import type { Metadata } from "next";
import Navbar from "@/components/Navbar";

export const metadata: Metadata = {
  title: "Crypto Portfolio",
  description: "Track real-time crypto prices and your portfolio",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="bg-black text-white">
      <body className="min-h-screen bg-[#0d0d0d] text-gray-100 antialiased">
        <Navbar />
        <main className="max-w-6xl mx-auto px-6 py-10">{children}</main>
      </body>
    </html>
  );
}
