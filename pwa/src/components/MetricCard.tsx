import { ReactNode } from 'react';

interface MetricCardProps {
  label: string;
  value: string;
  unit?: string;
  tone?: 'accent' | 'success' | 'warning' | 'neutral';
  icon: ReactNode;
}

export function MetricCard({ label, value, unit, tone = 'neutral', icon }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card--${tone}`}>
      <div className="metric-card__icon">{icon}</div>
      <div>
        <p className="overline">{label}</p>
        <p className="metric-card__value">
          {value}
          {unit ? <span>{unit}</span> : null}
        </p>
      </div>
    </article>
  );
}
