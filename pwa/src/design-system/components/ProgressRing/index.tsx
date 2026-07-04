import { ReactNode, memo } from 'react';

export interface ProgressRingProps {
  caption?: ReactNode;
  phaseColor?: string | undefined;
  phaseLabel: string;
  progress: number;
  unit: string;
  value: string;
}

function ProgressRingComponent({ caption, phaseColor = 'var(--text-secondary)', phaseLabel, progress, unit, value }: ProgressRingProps) {
  const degrees = Math.max(0, Math.min(100, progress)) * 3.6;

  return (
    <div
      aria-label={phaseLabel}
      aria-valuemax={100}
      aria-valuemin={0}
      aria-valuenow={Math.round(Math.max(0, Math.min(100, progress)))}
      className="sq-progress-ring"
      role="progressbar"
      style={{ background: `conic-gradient(var(--accent) 0deg ${degrees}deg, var(--border) ${degrees}deg 360deg)` }}
    >
      <div className="sq-progress-ring__inner">
        <span aria-live="polite" className="overline" style={{ color: phaseColor }}>
          {phaseLabel}
        </span>
        <strong>{value}</strong>
        <span className="sq-progress-ring__unit">{unit}</span>
        {caption ? <div className="sq-progress-ring__caption">{caption}</div> : null}
      </div>
    </div>
  );
}

export const ProgressRing = memo(ProgressRingComponent);
