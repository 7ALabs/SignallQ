import { ArrowRight } from 'lucide-react';
import { ReactNode } from 'react';
import { Card } from '../Card';

export interface ActionCardProps {
  description: string;
  icon?: ReactNode;
  meta?: string;
  title: string;
}

export function ActionCard({ description, icon, meta, title }: ActionCardProps) {
  return (
    <Card className="sq-action-card" variant="outlined">
      <div className="sq-action-card__icon">{icon}</div>
      <div>
        {meta ? <p className="sq-overline">{meta}</p> : null}
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      <ArrowRight aria-hidden="true" size={20} />
    </Card>
  );
}
