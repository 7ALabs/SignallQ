import { Card } from '../Card';
import { QualityBadge } from '../QualityBadge';

export interface ConnectionSummaryCardProps {
  description: string;
  quality: 'good' | 'fair' | 'poor' | 'unknown';
  qualityLabel: string;
  title: string;
}

export function ConnectionSummaryCard({ description, quality, qualityLabel, title }: ConnectionSummaryCardProps) {
  return (
    <Card className="sq-summary-card" variant="tonal">
      <div>
        <p className="sq-overline">Resumo</p>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <QualityBadge label={qualityLabel} level={quality} />
    </Card>
  );
}
