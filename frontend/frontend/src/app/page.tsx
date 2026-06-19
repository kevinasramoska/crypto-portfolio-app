import Link from "next/link";

const features = [
  {
    icon: "📊",
    title: "Track Transactions",
    description: "Record every buy and sell with precise prices and dates.",
  },
  {
    icon: "💼",
    title: "Monitor Holdings",
    description: "View your current positions and average buy prices at a glance.",
  },
  {
    icon: "📈",
    title: "Calculate Profit",
    description: "See realised and unrealised gains across your entire portfolio.",
  },
  {
    icon: "📉",
    title: "Performance History",
    description: "Review portfolio snapshots over 7d, 30d, and 90d periods.",
  },
];

export default function Home() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-900 via-black to-slate-900 text-white">
      {/* Hero Section */}
      <section className="relative overflow-hidden px-6 py-20 sm:py-28">
        <div className="mx-auto max-w-4xl space-y-8">
          <div className="space-y-4">
            <p className="text-sm font-semibold uppercase tracking-widest text-purple-400">
              Crypto Portfolio Tracker
            </p>
            <h1 className="text-5xl font-bold tracking-tight sm:text-6xl lg:text-7xl">
              Know your{" "}
              <span className="bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                crypto portfolio
              </span>
              .
            </h1>
            <p className="max-w-2xl text-lg leading-8 text-gray-300">
              Track every transaction. Monitor your holdings. Calculate real profit and loss. All in one place. Get
              clarity on what matters.
            </p>
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col gap-4 pt-4 sm:flex-row sm:gap-3">
            <Link
              href="/register"
              className="rounded-lg bg-gradient-to-r from-purple-600 to-purple-700 px-8 py-3 text-center font-semibold text-white transition hover:from-purple-500 hover:to-purple-600 focus:outline-none focus:ring-2 focus:ring-purple-400 focus:ring-offset-2 focus:ring-offset-black"
            >
              Get Started
            </Link>
            <Link
              href="/login"
              className="rounded-lg border border-purple-500/30 bg-purple-500/10 px-8 py-3 text-center font-semibold text-purple-100 transition hover:border-purple-400/50 hover:bg-purple-400/20 focus:outline-none focus:ring-2 focus:ring-purple-400 focus:ring-offset-2 focus:ring-offset-black"
            >
              Sign In
            </Link>
            <Link
              href="/dashboard"
              className="rounded-lg border border-gray-700 bg-gray-800/20 px-8 py-3 text-center font-semibold text-gray-200 transition hover:border-gray-600 hover:bg-gray-700/30 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 focus:ring-offset-black"
            >
              View Demo
            </Link>
          </div>
        </div>

        {/* Decorative gradient orb */}
        <div className="pointer-events-none absolute -right-40 -top-40 h-80 w-80 rounded-full bg-purple-500/20 blur-3xl" />
      </section>

      {/* Features Section */}
      <section className="relative px-6 py-20 sm:py-28">
        <div className="mx-auto max-w-6xl">
          <div className="mb-16 space-y-4 text-center">
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl">Everything you need</h2>
            <p className="mx-auto max-w-2xl text-lg text-gray-400">
              Powerful tools to manage your crypto investments with confidence.
            </p>
          </div>

          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">
            {features.map(feature => (
              <div
                key={feature.title}
                className="group relative rounded-xl border border-gray-800/50 bg-gradient-to-br from-gray-900/40 to-gray-950/40 p-6 transition hover:border-purple-500/30 hover:bg-purple-500/5"
              >
                <div className="space-y-3">
                  <div className="text-4xl">{feature.icon}</div>
                  <h3 className="font-semibold text-white">{feature.title}</h3>
                  <p className="text-sm leading-6 text-gray-400">{feature.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="relative px-6 py-20 sm:py-28">
        <div className="mx-auto max-w-4xl space-y-12">
          <div className="space-y-4 text-center">
            <h2 className="text-4xl font-bold tracking-tight">Built for clarity</h2>
            <p className="text-lg text-gray-400">Designed specifically for crypto traders who want transparency.</p>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            <div className="rounded-xl border border-gray-800/50 bg-gray-950/40 p-8 text-center">
              <div className="space-y-2">
                <p className="text-4xl font-bold text-purple-400">100%</p>
                <p className="text-sm text-gray-400">Realised & Unrealised P/L</p>
              </div>
            </div>
            <div className="rounded-xl border border-gray-800/50 bg-gray-950/40 p-8 text-center">
              <div className="space-y-2">
                <p className="text-4xl font-bold text-purple-400">12+</p>
                <p className="text-sm text-gray-400">Supported Coins</p>
              </div>
            </div>
            <div className="rounded-xl border border-gray-800/50 bg-gray-950/40 p-8 text-center">
              <div className="space-y-2">
                <p className="text-4xl font-bold text-purple-400">∞</p>
                <p className="text-sm text-gray-400">Transaction History</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA Section */}
      <section className="relative px-6 py-20">
        <div className="mx-auto max-w-2xl space-y-8 text-center">
          <div className="space-y-4">
            <h2 className="text-4xl font-bold tracking-tight">Ready to start?</h2>
            <p className="text-lg text-gray-400">
              Create an account in seconds and start tracking your crypto portfolio today.
            </p>
          </div>

          <div className="flex gap-3 justify-center">
            <Link
              href="/register"
              className="rounded-lg bg-gradient-to-r from-purple-600 to-purple-700 px-8 py-3 font-semibold text-white transition hover:from-purple-500 hover:to-purple-600 focus:outline-none focus:ring-2 focus:ring-purple-400 focus:ring-offset-2 focus:ring-offset-black"
            >
              Create Account
            </Link>
            <Link
              href="/login"
              className="rounded-lg border border-gray-700 bg-gray-800/20 px-8 py-3 font-semibold text-gray-200 transition hover:border-gray-600 hover:bg-gray-700/30 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 focus:ring-offset-black"
            >
              Log In
            </Link>
          </div>
        </div>

        {/* Decorative gradient orb */}
        <div className="pointer-events-none absolute -left-40 -bottom-40 h-80 w-80 rounded-full bg-purple-500/10 blur-3xl" />
      </section>
    </main>
  );
}
