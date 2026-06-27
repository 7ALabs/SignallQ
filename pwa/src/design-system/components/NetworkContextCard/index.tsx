import { Card } from '../Card';

export interface NetworkContextItem {
  label: string;
  value: string;
}

export interface NetworkContextCardProps {
  items: NetworkContextItem[];
  title: string;
}

export function NetworkContextCard({ items, title }: NetworkContextCardProps) {
  return (
    <Card className="sq-network-context-card" variant="outlined">
      <h3>{title}</h3>
      <dl>
        {items.map((item) => (
          <div key={item.label}>
            <dt>{item.label}</dt>
            <dd>{item.value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}
