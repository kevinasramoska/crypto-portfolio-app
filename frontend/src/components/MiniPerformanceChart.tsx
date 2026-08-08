"use client";

import React from "react";

type Point = {
  x: string; // ISO timestamp
  y1: number; // totalCurrentValueUsd
  y2: number; // totalProfitLossUsd
};

type Props = {
  points: Point[];
  height?: number;
  padding?: number;
};

export default function MiniPerformanceChart({ points, height = 200, padding = 24 }: Props) {
  if (!points || points.length === 0) {
    return (
      <div className="h-[200px] flex items-center justify-center text-sm text-gray-500">No chart data</div>
    );
  }

  const width = 800; // SVG viewBox width; will scale by CSS
  const innerWidth = width - padding * 2;
  const innerHeight = height - padding * 2;

  const valuesY1 = points.map(p => p.y1);
  const valuesY2 = points.map(p => p.y2);
  const allValues = [...valuesY1, ...valuesY2];
  const max = Math.max(...allValues);
  const min = Math.min(...allValues);
  const range = max - min || 1;

  const xForIndex = (i: number) => padding + (i / (points.length - 1 || 1)) * innerWidth;
  const yForValue = (v: number) => padding + innerHeight - ((v - min) / range) * innerHeight;

  const pathFor = (values: number[]) => {
    return values
      .map((v, i) => {
        const x = xForIndex(i);
        const y = yForValue(v);
        return `${i === 0 ? "M" : "L"} ${x.toFixed(2)} ${y.toFixed(2)}`;
      })
      .join(" ");
  };

  const pointsToCircles = (values: number[], color: string) => {
    return values.map((v, i) => {
      const x = xForIndex(i);
      const y = yForValue(v);
      return <circle key={i + color} cx={x} cy={y} r={2.5} fill={color} />;
    });
  };

  const formatShortDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleDateString();
  };

  return (
    <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-950/40 p-3">
      <svg viewBox={`0 0 ${width} ${height}`} width="100%" height={height} preserveAspectRatio="xMidYMid meet">
        {/* grid lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((t, idx) => (
          <line
            key={idx}
            x1={padding}
            x2={width - padding}
            y1={padding + innerHeight * t}
            y2={padding + innerHeight * t}
            stroke="rgba(148,163,184,0.06)"
            strokeWidth={1}
          />
        ))}

        {/* y labels (left) */}
        <g fill="#9CA3AF" fontSize={10}>
          <text x={6} y={padding + 10}>
            {max.toFixed(0)}
          </text>
          <text x={6} y={padding + innerHeight / 2 + 4}>
            {((max + min) / 2).toFixed(0)}
          </text>
          <text x={6} y={padding + innerHeight + 4}>
            {min.toFixed(0)}
          </text>
        </g>

        {/* lines */}
        <path d={pathFor(valuesY1)} fill="none" stroke="#7C3AED" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
        <path d={pathFor(valuesY2)} fill="none" stroke="#F59E0B" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />

        {/* small points */}
        {pointsToCircles(valuesY1, "#7C3AED")}
        {pointsToCircles(valuesY2, "#F59E0B")}

        {/* x labels */}
        <g fill="#6B7280" fontSize={10}>
          {points.map((p, i) => {
            // show first, last, and middle label only to avoid clutter
            if (i === 0 || i === points.length - 1 || i === Math.floor(points.length / 2)) {
              const x = xForIndex(i);
              const y = height - 6;
              return (
                <text key={p.x} x={x} y={y} textAnchor={i === points.length - 1 ? "end" : i === 0 ? "start" : "middle"}>
                  {formatShortDate(p.x)}
                </text>
              );
            }
            return null;
          })}
        </g>
      </svg>
    </div>
  );
}

