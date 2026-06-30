import type { Report } from './reportTypes';

interface ReportPageProps {
  error: string | null;
  isLoading: boolean;
  onBack: () => void;
  onCopyLink: () => void;
  report: Report | null;
  reportId: string;
}

function formatDate(timestampEpochMs: number): string {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(timestampEpochMs));
}

function statusLabel(status: Report['status']): string {
  switch (status) {
    case 'good':
      return 'Conexão boa';
    case 'attention':
      return 'Atenção';
    case 'critical':
      return 'Crítico';
    case 'inconclusive':
      return 'Inconclusivo';
  }
}

export function ReportPage({ error, isLoading, onBack, onCopyLink, report, reportId }: ReportPageProps) {
  return (
    <main className="report-page">
      <section className="report-page__hero">
        <p className="overline">SignallQ PWA</p>
        <h1>Laudo de conexão</h1>
        <p>Este laudo fica salvo apenas neste navegador.</p>
        <div className="report-page__actions">
          <button className="text-button" type="button" onClick={onBack}>
            Voltar
          </button>
          <button className="text-button" type="button" onClick={onCopyLink}>
            Copiar link
          </button>
        </div>
      </section>

      {isLoading ? <p aria-live="polite" className="report-page__message">Carregando laudo local...</p> : null}
      {error ? <p className="report-page__message report-page__message--error" role="alert">Erro ao abrir laudo: {error}</p> : null}
      {!isLoading && !error && !report ? (
        <section className="report-page__empty">
          <h2>Laudo não encontrado neste navegador</h2>
          <p>
            O link existe, mas os dados ficam no IndexedDB local. Se você abriu em outro aparelho, outro navegador ou
            limpou os dados do site, o laudo não estará disponível aqui.
          </p>
          <p className="overline">ID: {reportId}</p>
        </section>
      ) : null}

      {report ? (
        <article className={`report-card report-card--${report.status}`}>
          <div className="report-card__header">
            <div>
              <p className="overline">{formatDate(report.timestampEpochMs)}</p>
              <h2>{report.title}</h2>
              <p>{report.summary}</p>
            </div>
            <span>{statusLabel(report.status)}</span>
          </div>

          <div className="report-card__sections">
            {report.sections.map((section) => (
              <section key={section.title}>
                <h3>{section.title}</h3>
                <p>{section.body}</p>
              </section>
            ))}
          </div>

          <footer>
            <p>
              Compartilhamento remoto real exige backend futuro. Este link só recupera o laudo quando os dados locais
              ainda existem neste navegador.
            </p>
          </footer>
        </article>
      ) : null}
    </main>
  );
}
