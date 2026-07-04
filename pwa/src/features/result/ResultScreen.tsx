import type { DiagnosisResult, SpeedTestResult } from '@shared/contracts';
import { AppShell, Button, Icon, LimitationsCard, RecommendationList, StatusCard, TopAppBar } from '@/design-system';
import type { RecommendationListItem } from '@/design-system';
import type { StatusVerdict } from '@/design-system/components/StatusCard';

interface ResultScreenProps {
  diagnosis: DiagnosisResult | null;
  onCopyLink: () => void;
  onRetry: () => void;
  result: SpeedTestResult;
}

function verdictFromQuality(quality: DiagnosisResult['quality'] | undefined): StatusVerdict {
  switch (quality) {
    case 'good':
      return 'good';
    case 'attention':
      return 'attention';
    case 'bad':
      return 'bad';
    default:
      return 'unknown';
  }
}

function statusTitle(quality: DiagnosisResult['quality'] | undefined): string {
  switch (quality) {
    case 'good':
      return 'Conexão boa';
    case 'attention':
      return 'Conexão com atenção';
    case 'bad':
      return 'Conexão ruim';
    default:
      return 'Diagnóstico inconclusivo';
  }
}

function metricVerdict(value: number | null, warn: number, critical: number, inverse: boolean): { color: string; label: string } {
  if (value == null) return { color: 'var(--text-tertiary)', label: 'não medida' };
  const bad = inverse ? value >= critical : value <= critical;
  const okish = inverse ? value >= warn : value <= warn;
  if (bad) return { color: 'var(--error)', label: 'Fraca' };
  if (!okish) return { color: 'var(--warning)', label: 'Regular' };
  return { color: 'var(--success)', label: 'Boa' };
}

export function ResultScreen({ diagnosis, onCopyLink, onRetry, result }: ResultScreenProps) {
  const ping = metricVerdict(result.latency.ms, 80, 150, true);
  const jitter = metricVerdict(result.jitter.ms, 20, 40, true);
  const download = metricVerdict(result.download.mbps, 10, 3, false);
  const upload = metricVerdict(result.upload.mbps, 5, 1, false);

  const recommendations: RecommendationListItem[] =
    diagnosis?.actions.slice(0, 3).map((action) => ({
      description: action.description,
      icon:
        action.category === 'router'
          ? 'restart_alt'
          : action.category === 'wifi'
            ? 'wifi'
            : action.category === 'device'
              ? 'cable'
              : 'schedule',
      iconColor: diagnosis.quality === 'good' ? 'success' : 'accent',
      title: action.title,
    })) ?? [];

  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <>
              <Button icon={<Icon name="bookmark_add" size={16} />} onClick={onCopyLink} variant="outline">
                Salvar
              </Button>
              <Button icon={<Icon name="refresh" size={16} />} onClick={onRetry}>
                Refazer
              </Button>
            </>
          }
          mobileAction={
            <>
              <button aria-label="Salvar" className="sq-icon-button" onClick={onCopyLink} type="button">
                <Icon name="bookmark_add" size={19} />
              </button>
              <button aria-label="Refazer" className="sq-icon-button sq-icon-button--accent" onClick={onRetry} type="button">
                <Icon name="refresh" size={19} />
              </button>
            </>
          }
          mobileMode="back"
          mobileTitle="Resultado"
          onMobileBack={onRetry}
        />
      }
      maxWidth={800}
    >
      <div className="sq-result-screen">
        <StatusCard
          description={diagnosis?.summary ?? 'Medimos download, upload, latência e estabilidade da sua conexão pelo navegador.'}
          title={statusTitle(diagnosis?.quality)}
          verdict={verdictFromQuality(diagnosis?.quality)}
        />

        <div className="sq-result-screen__context">
          <Icon name="wifi" size={15} />
          <span>
            Via navegador
            {result.connection.effectiveType ? ` · Conexão ${result.connection.effectiveType}` : ''}
          </span>
        </div>

        <div className="sq-metrics-grid">
          <div className="sq-metric-block">
            <span className="overline">Ping</span>
            <strong>
              {result.latency.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: ping.color }}>
              {ping.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Jitter</span>
            <strong>
              {result.jitter.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: jitter.color }}>
              {jitter.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Download</span>
            <strong style={{ color: download.color }}>
              {result.download.mbps?.toFixed(0) ?? '--'}
              <span> Mbps</span>
            </strong>
            <span className="label-small" style={{ color: download.color }}>
              {download.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Upload</span>
            <strong style={{ color: upload.color }}>
              {result.upload.mbps?.toFixed(0) ?? '--'}
              <span> Mbps</span>
            </strong>
            <span className="label-small" style={{ color: upload.color }}>
              {upload.label}
            </span>
          </div>
        </div>

        {recommendations.length > 0 ? <RecommendationList items={recommendations} /> : null}

        {diagnosis && diagnosis.limitations.length > 0 ? <LimitationsCard items={diagnosis.limitations.map((item) => item.message)} /> : null}
      </div>
    </AppShell>
  );
}
