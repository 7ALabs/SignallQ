import { CheckCircle2 } from 'lucide-react';
import { Card } from '../Card';

export interface RecommendationListProps {
  items: string[];
  title: string;
}

export function RecommendationList({ items, title }: RecommendationListProps) {
  return (
    <Card className="sq-recommendation-list" variant="surface">
      <h3>{title}</h3>
      <ul>
        {items.map((item) => (
          <li key={item}>
            <CheckCircle2 aria-hidden="true" size={18} />
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </Card>
  );
}
