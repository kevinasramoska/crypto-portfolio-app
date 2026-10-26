"use client";

import Link from "next/link";
import { useState } from "react";
import AssetAllocationSummary from "./AssetAllocationSummary";
import MiniPerformanceChart from "./MiniPerformanceChart";
import PortfolioSummaryCards from "./PortfolioSummaryCards";
import TransactionsTable from "./TransactionsTable";
import { DEMO_PORTFOLIOS } from "@/lib/demoPortfolios";
import { formatUnitPrice } from "@/lib/format";

const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

export default function DemoPortfolios() {
  const [selected, setSelected] = useState("long-term");
  const portfolio = DEMO_PORTFOLIOS.find(item => item.id === selected) ?? DEMO_PORTFOLIOS[0];

  return (
    <main className="mx-auto max-w-7xl space-y-10 px-4 py-10 sm:px-6 sm:py-16">
      <header className="max-w-4xl space-y-5">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-purple-300">The two-minute product tour</p>
        <h1 className="text-4xl font-semibold tracking-tight sm:text-6xl">A portfolio with a story.<br /><span className="text-purple-300">Not an empty dashboard.</span></h1>
        <p className="max-w-2xl text-lg leading-8 text-gray-300">Explore three ways to use CryptoTracker. Follow the money from trade history to cost basis, allocation, and realised versus unrealised results.</p>
        <div className="flex flex-wrap gap-3 text-sm">
          <Link href="/engineering" className="rounded-lg bg-purple-600 px-4 py-2.5 font-semibold hover:bg-purple-500">Read the engineering story ↗</Link>
          <Link href="/register" className="rounded-lg border border-gray-700 px-4 py-2.5 hover:border-purple-400">Create your own portfolio</Link>
        </div>
      </header>

      <aside className="rounded-xl border border-amber-400/30 bg-amber-400/5 px-5 py-4 text-sm leading-6 text-amber-100">
        <strong>Read-only sample data.</strong> All trades, prices, and chart points are fictional illustrations, fixed as of 1 October 2026. No live market feed, signup, or account access is used. Switching portfolios never writes data.
      </aside>

      <div aria-label="Choose a demo portfolio" className="grid gap-3 md:grid-cols-3">
        {DEMO_PORTFOLIOS.map((item, index) => (
          <button key={item.id} type="button" aria-pressed={selected === item.id} onClick={() => setSelected(item.id)}
            className={`rounded-2xl border p-5 text-left transition ${selected === item.id ? "border-purple-400 bg-purple-500/10" : "border-gray-800 bg-gray-950/70 hover:border-gray-600"}`}>
            <span className="text-xs font-medium uppercase tracking-widest text-purple-300">0{index + 1}</span>
            <span className="mt-3 block text-xl font-semibold">{item.title}</span>
            <span className="mt-1 block text-sm text-gray-400">{item.subtitle}</span>
          </button>
        ))}
      </div>

      <section aria-live="polite" aria-label="Selected sample portfolio" className="min-w-0 space-y-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div><p className="text-xs uppercase tracking-widest text-purple-300">Sample portfolio</p><h2 className="mt-2 text-3xl font-semibold">{portfolio.title}</h2></div>
          <span className="text-sm text-gray-400">USD · illustrative valuation</span>
        </div>
        <PortfolioSummaryCards summary={portfolio.summary} sample />
        <div className="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)]">
          <section className="min-w-0 rounded-2xl border border-gray-800 bg-gray-950/60 p-5 sm:p-6">
            <h3 className="text-lg font-semibold">The 90-day picture</h3>
            <p className="mb-5 mt-1 text-sm text-gray-400">Illustrative values, not a backtest or investment return.</p>
            <MiniPerformanceChart points={portfolio.points} />
            <p className="mt-4 text-sm leading-6 text-gray-300">{portfolio.lesson}</p>
          </section>
          <section className="min-w-0 rounded-2xl border border-gray-800 bg-gray-950/60 p-5 sm:p-6"><h3 className="mb-5 text-lg font-semibold">Asset allocation</h3><AssetAllocationSummary holdings={portfolio.holdings} /></section>
        </div>
        <section className="space-y-4"><h3 className="text-xl font-semibold">Inside the positions</h3>
          <div className="grid gap-4 md:grid-cols-3">{portfolio.holdings.map(holding => (
            <article key={holding.symbol} className="rounded-xl border border-gray-800 bg-gray-950/70 p-5">
              <div className="flex items-center justify-between"><h4 className="font-semibold">{holding.symbol} <span className="text-sm font-normal text-gray-400">{holding.name}</span></h4><span className="text-xs text-purple-300">Sample</span></div>
              <p className="mt-4 text-2xl font-semibold">{money.format(holding.currentValueUsd)}</p>
              <dl className="mt-4 space-y-2 text-sm"><div className="flex justify-between gap-2"><dt className="text-gray-400">Quantity</dt><dd>{holding.quantity.toLocaleString("en-US")}</dd></div><div className="flex justify-between gap-2"><dt className="text-gray-400">Average entry</dt><dd>{formatUnitPrice(holding.averageBuyPriceUsd)}</dd></div><div className="flex justify-between gap-2"><dt className="text-gray-400">Unrealised P/L</dt><dd className={holding.profitLossUsd < 0 ? "text-red-300" : "text-emerald-300"}>{money.format(holding.profitLossUsd)}</dd></div></dl>
            </article>
          ))}</div>
        </section>
        <section className="min-w-0 space-y-4"><h3 className="text-xl font-semibold">The trades behind the totals</h3><p className="text-sm text-gray-400">Final sample entries only. Account editing and audit revisions are demonstrated in the engineering walkthrough.</p><TransactionsTable transactions={portfolio.transactions} /></section>
      </section>
      <section className="grid gap-6 rounded-2xl border border-purple-400/25 bg-purple-500/5 p-6 sm:grid-cols-2 sm:p-8">
        <div><p className="text-xs uppercase tracking-widest text-purple-300">Built to explain its numbers</p><h2 className="mt-3 text-2xl font-semibold">Explore the decisions behind the interface.</h2></div>
        <div className="space-y-4 text-sm leading-7 text-gray-300"><p>Java 25 + Spring Boot handle the real ledger. PostgreSQL protects revisions and concurrent writes. Next.js makes the results usable on desktop and mobile.</p><Link className="inline-block font-semibold text-purple-300 underline underline-offset-4" href="/engineering">Architecture, trade-offs, and verification →</Link></div>
      </section>
    </main>
  );
}
