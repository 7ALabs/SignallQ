import type { HistoryEntry } from '@shared/contracts';
import { AppShell, Button, Icon, LimitationsCard, RecommendationList, StatusCard, TopAppBar } from '@/design-system';
import type { RecommendationListItem } from '@/design-system';
import { metricVerdict, statusTitle, verdictFromQuality } from '@/shared/verdict';

interface TestDetailScreenProps {
  entry: HistoryEntry;
  onBack: () => void;
  onRemove: () => void;
  onRetry: () => void;
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
            <Button icon={<Icon name="arrow_back" size={16} />} onClick={onBack} variant="text">
              Voltar
            </Button>
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

        <div className="sq-metrics-card">
          <div className="sq-metrics-card__item">
            <span className="overline">Ping</span>
            <strong>
              {speedTest.latency.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: ping.color }}>
              {ping.label}
            </span>
          </div>
          <div className="sq-metrics-card__item">
            <span className="overline">Jitter</span>
            <strong>
              {speedTest.jitter.ms ?? '--'}
              <span> ms</span>
            </strong>
            <span className="label-small" style={{ color: jitter.color }}>
              {jitter.label}
            </span>
          </div>
          <div className="sq-metrics-card__item">
            <span className="overline">Download</span>
            <strong style={{ color: download.color }}>
              {speedTest.download.mbps?.toFixed(0) ?? '--'}
              <span> Mbps</span>
            </strong>
            <span className="label-small" style={{ color: download.color }}>
              {download.label}
            </span>
          </div>
          <div className="sq-metrics-card__item">
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

        {diagnosis.limitations.length > 0 ? (
          <LimitationsCard
            items={diagnosis.limitations.map((l) => l.message)}
            title="Limitações"
            tone={diagnosis.quality === 'good' ? 'neutral' : 'warning'}
          />
        ) : null}
      </div>
    </AppShell>
  );
}
