import React from "react";
import { Construction } from "lucide-react";
import { alpha } from "../../utils/color";

interface FeatureComingSoonProps {
  feature: string;
  reason?: string;
  compact?: boolean;
}

export const FeatureComingSoon: React.FC<FeatureComingSoonProps> = ({
  feature,
  reason,
  compact = false,
}) => {
  if (compact) {
    return (
      <div
        className="flex items-center gap-2 py-3 px-4 rounded-lg"
        style={{ background: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <Construction className="w-4 h-4 shrink-0" style={{ color: "var(--text-tertiary)" }} />
        <span className="text-xs font-sans" style={{ color: "var(--text-secondary)" }}>
          {feature}
          <span
            className="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-mono uppercase tracking-wider"
            style={{
              background: alpha("var(--border)", 40),
              color: "var(--text-tertiary)",
              border: "1px solid var(--border)",
            }}
          >
            Em breve
          </span>
        </span>
      </div>
    );
  }

  return (
    <div
      className="flex flex-col items-center justify-center gap-3 py-10 px-6 rounded-xl"
      style={{ background: "var(--bg-surface)", border: "1px solid var(--border)" }}
    >
      <Construction className="w-8 h-8" style={{ color: "var(--text-tertiary)" }} />
      <span className="text-sm font-medium font-sans" style={{ color: "var(--text-secondary)" }}>Em breve</span>
      {reason && (
        <span className="text-xs text-center max-w-xs font-sans" style={{ color: "var(--text-tertiary)" }}>{reason}</span>
      )}
    </div>
  );
};
