import { ReactNode } from 'react';
import { Card } from '../Card';

export interface MetricTileProps {
  helperText?: string;
  icon?: ReactNode;
  label: string;
  status?: 'good' | 'warning' | 'critical' | 'neutral';
  unit?: string;
  value: string;
}

export function MetricTile({ helperText, icon, label, status = 'neutral', unit, value }: MetricTileProps) {
  return (
    <Card className={`sq-metric-tile sq-metric-tile--${status}`} variant="outlined">
      <div className="sq-metric-tile__top">
        <p className="sq-overline">{label}</p>
        {icon ? <span className="sq-metric-tile__icon">{icon}</span> : null}
      </div>
      <p className="sq-metric-tile__value">
        {value}
        {unit ? <span>{unit}</span> : null}
      </p>
      {helperText ? <p className="sq-muted">{helperText}</p> : null}
    </Card>
  );
}
