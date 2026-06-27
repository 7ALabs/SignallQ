import { ReactNode } from 'react';

export interface HomeLayoutProps {
  actions: ReactNode;
  hero: ReactNode;
  insights: ReactNode;
  metrics: ReactNode;
  summary: ReactNode;
}

export function HomeLayout({ actions, hero, insights, metrics, summary }: HomeLayoutProps) {
  return (
    <div className="sq-home-layout">
      <section>{hero}</section>
      <section className="sq-home-layout__summary">{summary}</section>
      <section className="sq-home-layout__metrics" aria-label="Métricas principais">
        {metrics}
      </section>
      <section className="sq-home-layout__actions" aria-label="Ações contextuais">
        {actions}
      </section>
      <section className="sq-home-layout__insights" aria-label="Diagnóstico e contexto">
        {insights}
      </section>
    </div>
  );
}
