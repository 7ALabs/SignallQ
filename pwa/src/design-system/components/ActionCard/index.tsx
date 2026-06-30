import { ArrowRight } from 'lucide-react';
import { ReactNode } from 'react';
import { Card } from '../Card';

export interface ActionCardProps {
  description: string;
  icon?: ReactNode;
  meta?: string;
  onClick?: () => void;
  title: string;
}

export function ActionCard({ description, icon, meta, onClick, title }: ActionCardProps) {
  return (
    <Card className="sq-action-card" variant="outlined">
      <div className="sq-action-card__icon">{icon}</div>
      <div>
        {meta ? <p className="sq-overline">{meta}</p> : null}
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      {onClick ? (
        <button className="sq-action-card__button" type="button" onClick={onClick} aria-label={`Abrir ${title}`}>
          <ArrowRight aria-hidden="true" size={20} />
        </button>
      ) : (
        <ArrowRight aria-hidden="true" size={20} />
      )}
    </Card>
  );
}
