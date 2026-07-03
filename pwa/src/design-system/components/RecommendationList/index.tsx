import { Card } from '../Card';
import { Icon } from '../Icon';

export interface RecommendationListItem {
  description: string;
  icon: string;
  iconColor?: 'accent' | 'success';
  title: string;
}

export interface RecommendationListProps {
  items: RecommendationListItem[];
  title?: string;
}

export function RecommendationList({ items, title = 'Recomendações' }: RecommendationListProps) {
  return (
    <Card className="sq-recommendation-list" variant="surface">
      <span className="overline">{title}</span>
      <ul>
        {items.map((item) => (
          <li key={item.title}>
            <Icon name={item.icon} size={19} style={{ color: item.iconColor === 'success' ? 'var(--success)' : 'var(--accent)' }} />
            <div>
              <strong>{item.title}</strong>
              <span>{item.description}</span>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}
