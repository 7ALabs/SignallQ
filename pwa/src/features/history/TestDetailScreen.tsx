import type { HistoryEntry } from '@shared/contracts';
import { AppShell, Button, Icon, LimitationsCard, RecommendationList, StatusCard, TopAppBar } from '@/design-system';
import type { RecommendationListItem } from '@/design-system';
import type { StatusVerdict } from '@/design-system/components/StatusCard';

interface TestDetailScreenProps {
  entry: HistoryEntry;
  onBack: () => void;
  onRemove: () => void;
  onRetry: () => void;
}

function verdictFromQuality(quality: HistoryEntry['diagnosis']['quality']): StatusVerdict {
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

function statusTitle(quality: HistoryEntry['diagnosis']['quality']): string {
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

export function TestDetailScreen({ entry, onBack, onRemove, onRetry }: TestDetailScreenProps) {
  const { diagnosis, speedTest } = entry;
  const dateLabel = new Date(entry.createdAt).toLocaleString('pt-BR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

  const ping = metricVerdict(speedTest.latency.ms, 80, 150, true);
  const jitter = metricVerdict(speedTest.jitter.ms, 20, 40, true);
  const download = metricVerdict(speedTest.download.mbps, 10, 3, false);
  const upload = metricVerdict(speedTest.upload.mbps, 5, 1, false);

  const recommendations: RecommendationListItem[] = diagnosis.actions.slice(0, 3).map((action) => ({
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
  }));

  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <>
              <Button icon={<Icon name="delete_sweep" size={16} />} onClick={onRemove} variant="danger-outline">
                Apagar
              </Button>
              <Button icon={<Icon name="refresh" size={16} />} onClick={onRetry}>
                Refazer
              </Button>
            </>
          }
          leading={
            <button aria-label="Voltar ao histórico" className="sq-icon-button" onClick={onBack} type="button" style={{ border: 0 }}>
              <Icon name="arrow_back" size={22} />
            </button>
          }
          mobileAction={
            <button aria-label="Refazer" className="sq-icon-button sq-icon-button--accent" onClick={onRetry} type="button">
              <Icon name="refresh" size={19} />
            </button>
          }
          mobileMode="back"
          mobileTitle="Detalhe"
          onMobileBack={onBack}
        />
      }
      maxWidth={800}
    >
      <div className="sq-detail-screen">
        <div className="sq-result-screen__context">
          <Icon name="bookmark" size={16} />
          <span>Resultado salvo · {dateLabel} · não é uma medição atual</span>
        </div>

        <StatusCard description={diagnosis.summary} title={statusTitle(diagnosis.quality)} verdict={verdictFromQuality(diagnosis.quality)} />

        <div className="sq-metrics-grid">
          <div className="sq-metric-block">
            <span className="overline">Ping</span>
            <strong>
              {speedTest.latency.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: ping.color }}>
              {ping.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Jitter</span>
            <strong>
              {speedTest.jitter.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: jitter.color }}>
              {jitter.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Download</span>
            <strong style={{ color: download.color }}>
              {speedTest.download.mbps?.toFixed(0) ?? '--'}
              <span> Mbps</span>
            </strong>
            <span className="label-small" style={{ color: download.color }}>
              {download.label}
            </span>
          </div>
          <div className="sq-metric-block">
            <span className="overline">Upload</span>
            <strong style={{ color: upload.color }}>
              {speedTest.upload.mbps?.toFixed(0) ?? '--'}
              <span> Mbps</span>
            </strong>
            <span className="label-small" style={{ color: upload.color }}>
              {upload.label}
            </span>
          </div>
        </div>

        {recommendations.length > 0 ? <RecommendationList items={recommendations} /> : null}

        {diagnosis.limitations.length > 0 ? <LimitationsCard items={diagnosis.limitations.map((l) => l.message)} title="Limitações" /> : null}
      </div>
    </AppShell>
  );
}
