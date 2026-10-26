import type { Metadata } from "next";
import DemoPortfolios from "@/components/DemoPortfolios";

export const metadata: Metadata = { title: "Explore three demo portfolios", description: "No signup needed: explore clearly labelled sample crypto portfolios and their accounting stories." };
export default function DemoPage() { return <DemoPortfolios />; }
