"use client";

import React from "react";

const CX = 180, CY = 186, R = 136;
const ARC_LEN = Math.PI * R;
const ARC_PATH = `M ${CX - R} ${CY} A ${R} ${R} 0 0 1 ${CX + R} ${CY}`;
const SCALE = ["0", "1", "5", "10", "20", "30", "50", "75", "100"];

function point(radius: number, fraction: number) {
  const rad = ((180 - fraction * 180) * Math.PI) / 180;
  return { x: CX + radius * Math.cos(rad), y: CY - radius * Math.sin(rad) };
}

const TICKS = Array.from({ length: 49 }, (_, i) => {
  const f = i / 48;
  const major = i % 6 === 0;
  const a = point(R + 11, f);
  const b = point(R + (major ? 21 : 17), f);
  return {
    x1: a.x,
    y1: a.y,
    x2: b.x,
    y2: b.y,
    width: major ? 2 : 1,
    color: major
      ? "color-mix(in srgb, var(--text-tertiary) 45%, transparent)"
      : "color-mix(in srgb, var(--border) 28%, transparent)",
  };
});

const SCALE_LABELS = SCALE.map((text, i) => {
  const p = point(R + 36, i / 8);
  return {
    text,
    left: `${(p.x / 360) * 100}%`,
    top: `${(p.y / 210) * 100}%`,
  };
});

export function Velocimetro({
  fraction,
  phaseColor,
  isRunning,
  children,
}: {
  fraction: number;
  phaseColor: string;
  isRunning: boolean;
  children?: React.ReactNode;
}) {
  const needle = point(R - 2, fraction);
  const needleFrom = point(R - 36, fraction);
  const dashOffset = ARC_LEN * (1 - fraction);

  return (
    <div className="relative aspect-[360/210] w-full sm:w-[440px]">
      {children}
      
      {/* Glow effect when running or idle */}
      <div
        className="absolute left-[50%] top-[62%] w-[60%] aspect-square rounded-full pointer-events-none sq-gauge-glow"
        style={{
          background:
            "radial-gradient(circle, color-mix(in srgb, var(--accent) 30%, transparent), transparent 72%)",
        }}
      />

      <svg
        viewBox="0 0 360 210"
        className="absolute inset-0 w-full h-full block pointer-events-none"
      >
        {TICKS.map((t, i) => (
          <line
            key={i}
            x1={t.x1}
            y1={t.y1}
            x2={t.x2}
            y2={t.y2}
            stroke={t.color}
            strokeWidth={t.width}
            strokeLinecap="round"
          />
        ))}

        {/* Base Arc */}
        <path
          d={ARC_PATH}
          stroke="color-mix(in srgb, var(--border) 20%, transparent)"
          strokeWidth="12"
          fill="none"
          strokeLinecap="round"
        />

        {/* Active Arc & Needle (only when running) */}
        {isRunning && (
          <>
            <path
              d={ARC_PATH}
              stroke={phaseColor}
              strokeWidth="12"
              fill="none"
              strokeLinecap="round"
              strokeDasharray={ARC_LEN}
              strokeDashoffset={dashOffset}
              className="transition-[stroke-dashoffset] duration-100 ease-out"
            />
            <line
              x1={needleFrom.x}
              y1={needleFrom.y}
              x2={needle.x}
              y2={needle.y}
              stroke={phaseColor}
              strokeWidth="4"
              strokeLinecap="round"
              className="transition-[x2,y2,x1,y1] duration-100 ease-out"
            />
          </>
        )}
      </svg>

      {/* Scale Labels */}
      {SCALE_LABELS.map((l, i) => (
        <div
          key={i}
          className="absolute -translate-x-1/2 -translate-y-1/2 font-medium text-[12px] leading-[1] text-[color:var(--text-tertiary)]"
          style={{ left: l.left, top: l.top }}
        >
          {l.text}
        </div>
      ))}
    </div>
  );
}
