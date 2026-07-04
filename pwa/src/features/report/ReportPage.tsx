import { AppShell, Button, Card, Icon, QualityBadge, TopAppBar } from '@/design-system';
import type { QualityLevel } from '@/design-system/types';
import type { Report, ReportStatus } from './reportTypes';

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

function statusLabel(status: ReportStatus): string {
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

function statusLevel(status: ReportStatus): QualityLevel {
  switch (status) {
    case 'good':
      return 'good';
    case 'attention':
      return 'fair';
    case 'critical':
      return 'poor';
    case 'inconclusive':
      return 'unknown';
  }
}

export function ReportPage({ error, isLoading, onBack, onCopyLink, report, reportId }: ReportPageProps) {
  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <Button icon={<Icon name="link" size={16} />} onClick={onCopyLink} variant="outline">
              Copiar link
            </Button>
          }
          leading={
            <Button icon={<Icon name="arrow_back" size={16} />} onClick={onBack} variant="text">
              Voltar
            </Button>
          }
          mobileAction={
            <button aria-label="Copiar link" className="sq-icon-button" onClick={onCopyLink} type="button">
              <Icon name="link" size={19} />
            </button>
          }
          mobileMode="back"
          mobileTitle="Laudo"
          onMobileBack={onBack}
        />
      }
      maxWidth={800}
    >
      <div className="sq-report-page">
        <h1 className="headline-medium">Laudo de conexão</h1>
        <p className="body-medium">Este laudo fica salvo apenas neste navegador.</p>

        {isLoading ? (
          <p aria-live="polite" className="body-medium">
            Carregando laudo local...
          </p>
        ) : null}

        {error ? (
          <Card variant="outlined">
            <p role="alert">Erro ao abrir laudo: {error}</p>
          </Card>
        ) : null}

        {!isLoading && !error && !report ? (
          <Card variant="outlined">
            <h2 className="title-large">Laudo não encontrado neste navegador</h2>
            <p className="body-medium">
              O link existe, mas os dados ficam no IndexedDB local. Se você abriu em outro aparelho, outro navegador ou
              limpou os dados do site, o laudo não estará disponível aqui.
            </p>
            <span className="overline">ID: {reportId}</span>
          </Card>
        ) : null}

        {report ? (
          <Card variant="surface">
            <div className="sq-report-page__header">
              <div>
                <span className="overline">{formatDate(report.timestampEpochMs)}</span>
                <h2 className="title-large">{report.title}</h2>
                <p className="body-medium">{report.summary}</p>
              </div>
              <QualityBadge label={statusLabel(report.status)} level={statusLevel(report.status)} />
            </div>

            <div className="sq-report-page__sections">
              {report.sections.map((section) => (
                <section key={section.title}>
                  <h3 className="title-small">{section.title}</h3>
                  <p className="body-medium">{section.body}</p>
                </section>
              ))}
            </div>

            <p className="body-small sq-report-page__footnote">
              Compartilhamento remoto real exige backend futuro. Este link só recupera o laudo quando os dados locais
              ainda existem neste navegador.
            </p>
          </Card>
        ) : null}
      </div>
    </AppShell>
  );
}
