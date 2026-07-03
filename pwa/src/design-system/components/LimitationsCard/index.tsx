export interface LimitationsCardProps {
  items: string[];
  title?: string;
}

export function LimitationsCard({ items, title = 'Limitações do teste' }: LimitationsCardProps) {
  return (
    <div className="sq-limitations-card">
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
