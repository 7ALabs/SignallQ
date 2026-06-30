import type { HistoryEntry } from '@shared/contracts';
import type { HistoryState } from './historyTypes';

interface HistoryPanelProps {
  onClear: () => void;
  onCopyReportLink: (id: string) => void;
  onOpenReport: (id: string) => void;
  onRemove: (id: string) => void;
  onStartTest?: () => void;
  state: HistoryState;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatMbps(value: number | null): string {
  return value == null ? 'Não medido' : `${value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} Mbps`;
}

function qualityLabel(value: HistoryEntry['diagnosis']['quality']): string {
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

function renderEntry(
  entry: HistoryEntry,
  onCopyReportLink: (id: string) => void,
  onOpenReport: (id: string) => void,
  onRemove: (id: string) => void,
) {
  return (
    <article className="history-entry" key={entry.id}>
      <div>
        <p className="overline">{formatDate(entry.createdAt)}</p>
        <h4>{entry.diagnosis.summary}</h4>
      </div>
      <dl>
        <div>
          <dt>Download</dt>
          <dd>{formatMbps(entry.speedTest.download.mbps)}</dd>
        </div>
        <div>
          <dt>Upload</dt>
          <dd>{formatMbps(entry.speedTest.upload.mbps)}</dd>
        </div>
        <div>
          <dt>Qualidade</dt>
          <dd>{qualityLabel(entry.diagnosis.quality)}</dd>
        </div>
      </dl>
      <div className="history-entry__actions">
        <button className="text-button" type="button" onClick={() => onOpenReport(entry.id)}>
          Abrir laudo
        </button>
        <button className="text-button" type="button" onClick={() => onCopyReportLink(entry.id)}>
          Copiar link
        </button>
        <button className="text-button text-button--danger" type="button" onClick={() => onRemove(entry.id)}>
          Remover
        </button>
      </div>
    </article>
  );
}

export function HistoryPanel({ onClear, onCopyReportLink, onOpenReport, onRemove, onStartTest, state }: HistoryPanelProps) {
  return (
    <section className="history-panel" aria-label="Histórico local">
      <div className="history-panel__header">
        <div>
          <p className="overline">Histórico local</p>
          <h3>Medições neste navegador</h3>
          <p>Seu histórico fica salvo neste navegador. Se você limpar os dados ou usar outro dispositivo, ele pode não aparecer.</p>
        </div>
        <button className="text-button" disabled={state.entries.length === 0} type="button" onClick={onClear}>
          Limpar tudo
        </button>
      </div>

      {state.status === 'loading' ? <p aria-live="polite" className="history-panel__message">Carregando histórico local...</p> : null}
      {state.status === 'error' ? (
        <div className="history-panel__message history-panel__message--error" role="alert">
          <strong>Histórico indisponível</strong>
          <p>{state.error}</p>
          {onStartTest ? (
            <button className="text-button" type="button" onClick={onStartTest}>
              Fazer novo teste
            </button>
          ) : null}
        </div>
      ) : null}
      {state.status === 'empty' || (state.status === 'idle' && state.entries.length === 0) ? (
        <div className="history-panel__message">
          <strong>Nenhuma medição salva ainda</strong>
          <p>Faça um teste para criar o primeiro laudo local neste navegador.</p>
          {onStartTest ? (
            <button className="text-button" type="button" onClick={onStartTest}>
              Iniciar teste
            </button>
          ) : null}
        </div>
      ) : null}
      {state.entries.length > 0 ? (
        <div className="history-panel__list">
          {state.entries.map((entry) => renderEntry(entry, onCopyReportLink, onOpenReport, onRemove))}
        </div>
      ) : null}
    </section>
  );
}
