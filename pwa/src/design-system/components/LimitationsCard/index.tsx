export interface LimitationsCardProps {
  items: string[];
  title?: string;
  tone?: 'warning' | 'neutral';
}

export function LimitationsCard({ items, title = 'Limitações do teste', tone = 'warning' }: LimitationsCardProps) {
  return (
    <div className={`sq-limitations-card sq-limitations-card--${tone}`}>
      <span className="overline sq-limitations-card__title">{title}</span>
      <div className="sq-limitations-card__list">
        {items.map((item) => (
          <span className="body-small" key={item}>
            · {item}
          </span>
        ))}
      </div>
    </div>
  );
}
