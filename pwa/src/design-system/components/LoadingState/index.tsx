export interface LoadingStateProps {
  label?: string;
}

export function LoadingState({ label = 'Carregando informações' }: LoadingStateProps) {
  return (
    <div aria-busy="true" aria-label={label} className="sq-loading-state">
      <div className="sq-skeleton sq-skeleton--title" />
      <div className="sq-skeleton sq-skeleton--body" />
      <div className="sq-skeleton sq-skeleton--body-short" />
    </div>
  );
}
