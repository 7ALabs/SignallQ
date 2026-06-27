import { Gauge } from 'lucide-react';
import { ReactNode } from 'react';
import { Card } from '../Card';
import { QualityBadge } from '../QualityBadge';

export interface SpeedHeroCardProps {
  action?: ReactNode;
  caption: string;
  downloadLabel: string;
  qualityLabel: string;
  stabilityLabel: string;
  title: string;
  value: string;
}

export function SpeedHeroCard({
  action,
  caption,
  downloadLabel,
  qualityLabel,
  stabilityLabel,
  title,
  value,
}: SpeedHeroCardProps) {
  return (
    <Card className="sq-speed-hero-card" variant="surface">
      <div className="sq-speed-hero-card__content">
        <div>
          <p className="sq-overline">Teste de conexão</p>
          <h1>{title}</h1>
          <p>{caption}</p>
        </div>
        <div className="sq-speed-hero-card__metric" aria-label={downloadLabel}>
          <Gauge aria-hidden="true" size={28} />
          <strong>{value}</strong>
          <span>{downloadLabel}</span>
        </div>
      </div>
      <div className="sq-speed-hero-card__footer">
        <QualityBadge label={qualityLabel} level="unknown" />
        <span>{stabilityLabel}</span>
        {action ? <div className="sq-speed-hero-card__action">{action}</div> : null}
      </div>
    </Card>
  );
}
