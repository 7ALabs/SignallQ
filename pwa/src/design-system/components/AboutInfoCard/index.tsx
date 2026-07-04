import { Icon } from '../Icon';

export interface AboutInfoCardProps {
  body: string;
  color: 'accent' | 'success' | 'warning';
  icon: string;
  title: string;
}

export function AboutInfoCard({ body, color, icon, title }: AboutInfoCardProps) {
  return (
    <div className="sq-about-info-card">
      <div className="sq-about-info-card__header">
        <span className={`sq-about-info-card__icon sq-about-info-card__icon--${color}`}>
          <Icon name={icon} size={24} />
        </span>
        <strong>{title}</strong>
      </div>
      <p className="body-medium">{body}</p>
    </div>
  );
}
