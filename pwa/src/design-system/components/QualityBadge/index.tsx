import { QualityLevel } from '../../tokens/colors';

export interface QualityBadgeProps {
  label: string;
  level?: QualityLevel;
}

export function QualityBadge({ label, level = 'unknown' }: QualityBadgeProps) {
  return <span className={`sq-quality-badge sq-quality-badge--${level}`}>{label}</span>;
}
