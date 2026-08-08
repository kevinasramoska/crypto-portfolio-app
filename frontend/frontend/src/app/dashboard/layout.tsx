import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Dashboard",
  description: "Track real-time crypto prices and your portfolio",
};

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10 lg:py-12">{children}</main>
  );
}
