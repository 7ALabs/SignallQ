import { ReactNode } from 'react';

export interface ProgressRingProps {
  caption?: ReactNode;
  phaseColor?: string | undefined;
  phaseLabel: string;
  progress: number;
  unit: string;
  value: string;
}

export function ProgressRing({ caption, phaseColor = 'var(--text-tertiary)', phaseLabel, progress, unit, value }: ProgressRingProps) {
  const degrees = Math.max(0, Math.min(100, progress)) * 3.6;

  return (
    <div
      className="sq-progress-ring"
      style={{ background: `conic-gradient(var(--accent) 0deg ${degrees}deg, var(--border) ${degrees}deg 360deg)` }}
    >
      <div className="sq-progress-ring__inner">
        <span className="overline" style={{ color: phaseColor }}>
          {phaseLabel}
        </span>
        <strong>{value}</strong>
        <span className="sq-progress-ring__unit">{unit}</span>
        {caption ? <div className="sq-progress-ring__caption">{caption}</div> : null}
      </div>
    </div>
  );
}
