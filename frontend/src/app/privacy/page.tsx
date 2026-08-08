import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Privacy",
  description: "How CryptoTracker handles account and portfolio data.",
};

const contactEmail = process.env.NEXT_PUBLIC_CONTACT_EMAIL;

export default function PrivacyPage() {
  return (
    <main className="mx-auto min-h-[calc(100vh-12rem)] max-w-3xl px-4 py-12 sm:px-6 sm:py-16">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-purple-300">Privacy</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight text-white">Your portfolio data</h1>
      <div className="mt-8 space-y-7 rounded-2xl border border-gray-800 bg-gray-950/60 p-6 text-sm leading-7 text-gray-300 sm:p-8">
        <section>
          <h2 className="text-lg font-semibold text-white">What the app stores</h2>
          <p className="mt-2">CryptoTracker stores your email address, a hashed password, and the transactions and holdings you enter so it can provide your portfolio view.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-white">How data is used</h2>
          <p className="mt-2">Your data is used only to operate this portfolio tracker. Live market prices are requested from the configured market-data provider. Do not enter recovery phrases, private keys, exchange passwords, or other wallet credentials.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-white">Your choices</h2>
          <p className="mt-2">You can log out at any time. To ask about access to or deletion of your account data, contact the project owner{contactEmail ? <> at <a className="text-purple-300 hover:text-white hover:underline" href={`mailto:${contactEmail}`}>{contactEmail}</a></> : " using the contact details provided with this deployment"}.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-white">Important note</h2>
          <p className="mt-2">CryptoTracker is a personal tracking tool, not a financial, tax, or investment advisor. Market data may be delayed, unavailable, or incomplete.</p>
        </section>
      </div>
    </main>
  );
}
