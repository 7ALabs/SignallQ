import { AlertTriangle } from 'lucide-react';
import { Button } from '../Button';
import { Card } from '../Card';

export interface ErrorStateProps {
  actionLabel?: string;
  description: string;
  title: string;
}

export function ErrorState({ actionLabel, description, title }: ErrorStateProps) {
  return (
    <Card className="sq-state-card sq-state-card--error" variant="outlined">
      <div className="sq-state-card__icon">
        <AlertTriangle aria-hidden="true" size={22} />
      </div>
      <h3>{title}</h3>
      <p>{description}</p>
      {actionLabel ? <Button variant="tonal">{actionLabel}</Button> : null}
    </Card>
  );
}
