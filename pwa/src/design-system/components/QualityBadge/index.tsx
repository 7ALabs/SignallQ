import { QualityLevel } from '../../tokens/colors';
import { Icon } from '../Icon';

const LEVEL_ICON: Record<QualityLevel, string> = {
  good: 'check_circle',
  fair: 'warning',
  poor: 'error',
  unknown: 'help',
};

export interface QualityBadgeProps {
  icon?: string;
  label: string;
  level?: QualityLevel;
}

export function QualityBadge({ icon, label, level = 'unknown' }: QualityBadgeProps) {
  return (
    <span className={`sq-quality-badge sq-quality-badge--${level}`}>
      <Icon name={icon ?? LEVEL_ICON[level]} size={15} />
      {label}
    </span>
  );
}
