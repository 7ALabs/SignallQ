import { memo } from 'react';
import { Icon } from '../Icon';

export type StepStatus = 'done' | 'active' | 'pending';

export interface StepTrackerItem {
  icon: string;
  key: string;
  label: string;
  status: StepStatus;
  value?: string;
}

export interface StepTrackerProps {
  items: StepTrackerItem[];
}

function StepTrackerComponent({ items }: StepTrackerProps) {
  return (
    <div className="sq-step-tracker">
      {items.map((item) => (
        <div className={`sq-step-tracker__item sq-step-tracker__item--${item.status}`} key={item.key}>
          <span className="sq-step-tracker__label">
            {item.status === 'active' ? <span className="sq-step-tracker__dot" /> : <Icon name={item.icon} size={17} />}
            {item.label}
          </span>
          {item.value ? <span className="sq-step-tracker__value">{item.value}</span> : null}
        </div>
      ))}
    </div>
  );
}

export const StepTracker = memo(StepTrackerComponent);
