import type { DiagnosisResult } from '@shared/contracts';

interface DiagnosisResultPanelProps {
  diagnosis: DiagnosisResult | null;
}

function qualityLabel(value: DiagnosisResult['quality']): string {
  switch (value) {
    case 'good':
      return 'Boa';
    case 'attention':
      return 'Atenção';
    case 'bad':
      return 'Ruim';
    case 'unknown':
      return 'Inconclusiva';
  }
}

function sourceLabel(value: DiagnosisResult['source']): string {
  switch (value) {
    case 'ai':
      return 'IA';
    case 'fallback':
      return 'local de contingência';
    case 'local':
      return 'local';
  }
}

export function DiagnosisResultPanel({ diagnosis }: DiagnosisResultPanelProps) {
  if (!diagnosis) return null;

  return (
    <section className="diagnosis-panel" aria-label="Resultado do diagnóstico">
      <div>
        <p className="overline">Diagnóstico {sourceLabel(diagnosis.source)}</p>
        <h3>{qualityLabel(diagnosis.quality)}</h3>
        <p>{diagnosis.summary}</p>
      </div>

      {diagnosis.actions.length > 0 ? (
        <div className="diagnosis-panel__actions">
          {diagnosis.actions.slice(0, 3).map((action) => (
            <article className="diagnosis-action" key={`${action.priority}-${action.title}`}>
              <p className="overline">Prioridade {action.priority} / {action.category}</p>
              <h4>{action.title}</h4>
              <p>{action.description}</p>
            </article>
          ))}
        </div>
      ) : null}

      {diagnosis.limitations.length > 0 ? (
        <details className="diagnosis-panel__details">
          <summary>Limitações consideradas</summary>
          <ul>
            {diagnosis.limitations.map((limitation) => (
              <li key={limitation.code}>{limitation.message}</li>
            ))}
          </ul>
        </details>
      ) : null}
    </section>
  );
}
