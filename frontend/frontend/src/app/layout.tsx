import "./globals.css";

export const metadata = {
  title: "Crypto Portfolio",
  description: "Track your crypto assets",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="bg-white text-black">{children}</body>
    </html>
  );
}
