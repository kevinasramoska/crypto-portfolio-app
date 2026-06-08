import Link from "next/link";

const features = [
  "Track BUY/SELL transactions",
  "View current holdings",
  "Track realised and unrealised profit/loss",
  "Review portfolio history",
];

export default function Home() {
  return (
    <main className="min-h-screen bg-black px-6 py-10 text-white">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-6xl flex-col justify-center gap-12">
        <section className="max-w-3xl space-y-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-purple-300">Crypto Portfolio Tracker</p>
          <div className="space-y-4">
            <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">
              Track your crypto portfolio from transactions to performance.
            </h1>
            <p className="max-w-2xl text-base leading-7 text-gray-400">
              Record buys and sells, monitor open holdings, and keep profit/loss visible without losing sight of
              unsupported market data.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href="/dashboard"
              className="rounded-lg bg-purple-600 px-5 py-3 text-center text-sm font-semibold text-white transition hover:bg-purple-500"
            >
              Open dashboard
            </Link>
            <Link
              href="/login"
              className="rounded-lg border border-gray-800 px-5 py-3 text-center text-sm font-semibold text-gray-200 transition hover:border-purple-500 hover:text-white"
            >
              Login
            </Link>
            <Link
              href="/register"
              className="rounded-lg border border-gray-800 px-5 py-3 text-center text-sm font-semibold text-gray-200 transition hover:border-purple-500 hover:text-white"
            >
              Register
            </Link>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {features.map(feature => (
            <div key={feature} className="rounded-xl border border-gray-800 bg-gray-950/60 p-5">
              <p className="text-sm font-semibold text-white">{feature}</p>
            </div>
          ))}
        </section>
      </div>
    </main>
  );
}
