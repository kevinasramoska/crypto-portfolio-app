import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Engineering the ledger", description: "Architecture, accounting trade-offs, revision safety, and verification behind CryptoTracker." };
const decisions = [
  ["01", "A ledger, not a mutable balance", "Creates, edits, and deletes replay active trade revisions into a holdings read model. Replacing an entry preserves the previous revision; invalid historical sells roll back the entire change."],
  ["02", "Execution time ≠ record time", "An optional execution timestamp is stored in UTC, separately from the original audit timestamp. Replay orders by execution time, then stable ledger sequence. Backdating is supported; future trades and historical oversells are rejected."],
  ["03", "Precision before presentation", "Quantity uses eight decimal places; prices and average costs use twelve, with twenty-place cost basis and realised-profit allocation. USD totals round to cents at their defined output boundaries—not before multiplying a small unit price."],
  ["04", "Concurrency has an owner", "A database row lock serializes each user's writes. Active-revision and holding constraints protect invariants; concurrent users can safely introduce the same asset into the shared catalog."],
  ["05", "Market uncertainty stays visible", "Quotes expire, cached fallback has a maximum age, and upstream requests have timeouts and backoff. Stale, missing, or uncertified observations are labelled rather than shown as trustworthy zero-value history."],
  ["06", "Queries stay on the server", "Owner-scoped symbol/type/date filters and allowlisted sorts run in PostgreSQL before pagination. CSV uses one bounded, repeatable-read snapshot so concurrent edits cannot shift rows between export pages."],
];

export default function EngineeringPage() {
  return (
    <main className="mx-auto max-w-6xl space-y-14 px-4 py-12 sm:px-6 sm:py-20">
      <header className="max-w-3xl space-y-6">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-purple-300">Behind the product</p>
        <h1 className="text-4xl font-semibold tracking-tight sm:text-6xl">The interface is simple.<br /><span className="text-purple-300">The numbers have rules.</span></h1>
        <p className="text-lg leading-8 text-gray-300">A full-stack engineering case study in financial precision, auditable edits, database concurrency, and honest market-data states.</p>
        <div className="flex flex-wrap gap-3"><Link href="/demo" className="rounded-lg bg-purple-600 px-5 py-3 font-semibold hover:bg-purple-500">Explore the sample portfolios</Link><a href="https://github.com/kevinasramoska/crypto-portfolio-app" className="rounded-lg border border-gray-700 px-5 py-3 hover:border-purple-400">View source ↗</a></div>
      </header>
      <section aria-label="Application architecture" className="grid gap-4 md:grid-cols-3">
        {[
          ["Browser", "Next.js + React", "Accessible forms, filter controls, request-generation guards, and an isolated sample-data preview."],
          ["API", "Java 25 + Spring Boot", "JWT authorization → controller → service. Validate, lock, revise, replay, and commit."],
          ["Data", "PostgreSQL + Flyway", "Ledger revisions, derived holdings, schema constraints, UTC execution times, and quality-labelled snapshots."],
        ].map(([layer, title, body]) => <article key={layer} className="rounded-2xl border border-purple-400/25 bg-purple-500/5 p-6"><p className="text-xs uppercase tracking-widest text-purple-300">{layer}</p><h2 className="mt-3 text-xl font-semibold">{title}</h2><p className="mt-3 text-sm leading-7 text-gray-300">{body}</p></article>)}
      </section>
      <section className="space-y-6"><h2 className="text-3xl font-semibold">Decisions worth discussing</h2><div className="grid gap-4 md:grid-cols-2">{decisions.map(([number, title, body]) => <article key={number} className="rounded-2xl border border-gray-800 bg-gray-950/60 p-6"><p className="text-xs text-purple-300">{number}</p><h3 className="mt-3 text-xl font-semibold">{title}</h3><p className="mt-3 text-sm leading-7 text-gray-300">{body}</p></article>)}</div></section>
      <section className="rounded-2xl border border-gray-800 bg-gray-950/60 p-6 sm:p-8">
        <h2 className="text-2xl font-semibold">A correction without rewriting the past</h2>
        <ol className="mt-5 space-y-4 text-sm leading-7 text-gray-300">
          <li><strong className="text-white">1. Record:</strong> buy 2 units at $10, then sell 1 at $20. Remaining cost is $10; realised profit is $10.</li>
          <li><strong className="text-white">2. Correct:</strong> replace the original buy with a $12 entry. Keep its previous revision, replay the active trades, and obtain $12 remaining cost and $8 realised profit.</li>
          <li><strong className="text-white">3. Protect:</strong> move that buy after the sell. The replay would oversell, so the entire edit rolls back. The valid ledger remains unchanged.</li>
        </ol>
        <p className="mt-5 text-sm text-gray-400">Affected snapshots are invalidated, not invented retroactively. New observations resume after the committed change.</p>
      </section>
      <section className="grid gap-8 md:grid-cols-2">
        <div><h2 className="text-2xl font-semibold">Evidence, not just a diagram</h2><ul className="mt-4 list-disc space-y-3 pl-5 text-sm leading-7 text-gray-300"><li>Real PostgreSQL migration, concurrency, rollback, precision, export-boundary, and restore tests.</li><li>HTTP checks for ownership isolation, backdated execution, invalid filters, and audit-preserving revisions.</li><li>Browser checks for registration → trade → edit → filtered CSV → delete, plus populated mobile widths.</li><li>Automated dependency and container advisory gates alongside build, unit, integration, and browser checks.</li></ul></div>
        <div><h2 className="text-2xl font-semibold">Deliberate limits</h2><p className="mt-4 text-sm leading-7 text-gray-300">This is a personal tracker, not an exchange or tax engine. History is point-in-time valuation, not a cash-flow-adjusted investment return. Ledger replay remains linear; history/export is capped at 10,000 matching rows.</p><p className="mt-4 text-sm leading-7 text-gray-300">The public demos are deterministic frontend fixtures, not seeded customer accounts or proof of a live backend. Account verification/recovery, production restoration, trusted proxies, and deployment still have explicit launch gates.</p></div>
      </section>
    </main>
  );
}
