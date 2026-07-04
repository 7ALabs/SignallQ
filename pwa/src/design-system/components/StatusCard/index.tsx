import { memo } from 'react';
import { Icon } from '../Icon';

export type StatusVerdict = 'good' | 'attention' | 'bad' | 'unknown';

export interface StatusCardProps {
  description: string;
  title: string;
  verdict: StatusVerdict;
}

const VERDICT_ICON: Record<StatusVerdict, string> = {
  good: 'check_circle',
  attention: 'warning',
  bad: 'error',
  unknown: 'help',
};

function StatusCardComponent({ description, title, verdict }: StatusCardProps) {
  return (
    <div className={`sq-status-card sq-status-card--${verdict}`}>
      <div className="sq-status-card__icon">
        <Icon name={VERDICT_ICON[verdict]} size={30} />
      </div>
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
    </div>
  );
}

export const StatusCard = memo(StatusCardComponent);
