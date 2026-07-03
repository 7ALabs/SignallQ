import { ReactNode } from 'react';
import { Card } from '../Card';
import { Icon } from '../Icon';

export interface ActionCardProps {
  action?: ReactNode;
  description: string;
  icon?: ReactNode;
  meta?: string;
  onClick?: () => void;
  title: string;
}

export function ActionCard({ action, description, icon, meta, onClick, title }: ActionCardProps) {
  const trailingAction =
    action ??
    (onClick ? (
      <button className="sq-action-card__button" type="button" onClick={onClick} aria-label={`Abrir ${title}`}>
        <Icon name="chevron_right" size={20} />
      </button>
    ) : (
      <Icon name="chevron_right" size={20} />
    ));

  return (
    <Card className="sq-action-card" variant="outlined">
      <div className="sq-action-card__icon">{icon}</div>
      <div>
        {meta ? <p className="sq-overline">{meta}</p> : null}
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      {trailingAction}
    </Card>
  );
}
