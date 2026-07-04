import { AppShell, Button, Icon, ProgressRing, StepTracker, TopAppBar } from '@/design-system';
import type { StepTrackerItem } from '@/design-system';
import { SpeedtestPhase } from '@/types/network';
import type { SpeedTestProgress, SpeedTestRunStatus } from './speedTestTypes';

interface SpeedTestScreenProps {
  onCancel: () => void;
  progress: SpeedTestProgress | null;
  status: SpeedTestRunStatus;
}

const PHASE_LABEL: Record<SpeedtestPhase, string> = {
  [SpeedtestPhase.Idle]: 'Preparando',
  [SpeedtestPhase.Latency]: 'Latência',
  [SpeedtestPhase.Download]: 'Download',
  [SpeedtestPhase.Upload]: 'Upload',
  [SpeedtestPhase.Partial]: 'Parcial',
  [SpeedtestPhase.Complete]: 'Concluído',
  [SpeedtestPhase.Error]: 'Erro',
  [SpeedtestPhase.Canceled]: 'Cancelado',
};

const PHASE_COLOR: Partial<Record<SpeedtestPhase, string>> = {
  [SpeedtestPhase.Latency]: 'var(--phase-latencia)',
  [SpeedtestPhase.Download]: 'var(--phase-download)',
  [SpeedtestPhase.Upload]: 'var(--phase-upload)',
};

const PHASE_PROGRESS: Record<SpeedtestPhase, number> = {
  [SpeedtestPhase.Idle]: 5,
  [SpeedtestPhase.Latency]: 20,
  [SpeedtestPhase.Download]: 55,
  [SpeedtestPhase.Upload]: 88,
  [SpeedtestPhase.Partial]: 100,
  [SpeedtestPhase.Complete]: 100,
  [SpeedtestPhase.Error]: 0,
  [SpeedtestPhase.Canceled]: 0,
};

const PHASE_ORDER = [SpeedtestPhase.Latency, SpeedtestPhase.Download, SpeedtestPhase.Upload];

function stepStatus(current: SpeedtestPhase, target: SpeedtestPhase, order: SpeedtestPhase[]): 'done' | 'active' | 'pending' {
  const currentIndex = order.indexOf(current);
  const targetIndex = order.indexOf(target);
  if (currentIndex > targetIndex) return 'done';
  if (currentIndex === targetIndex) return 'active';
  return 'pending';
}

export function SpeedTestScreen({ onCancel, progress, status }: SpeedTestScreenProps) {
  const phase = progress?.phase ?? SpeedtestPhase.Idle;
  const steps: StepTrackerItem[] = [
    { icon: 'check_circle', key: 'latency', label: 'Latência', status: stepStatus(phase, SpeedtestPhase.Latency, PHASE_ORDER) },
    { icon: 'download', key: 'download', label: 'Download', status: stepStatus(phase, SpeedtestPhase.Download, PHASE_ORDER) },
    { icon: 'upload', key: 'upload', label: 'Upload', status: stepStatus(phase, SpeedtestPhase.Upload, PHASE_ORDER) },
  ];

  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <Button icon={<Icon name="close" size={16} />} onClick={onCancel} variant="outline">
              Cancelar
            </Button>
          }
          mobileAction={
            <button aria-label="Cancelar" className="sq-icon-button" onClick={onCancel} type="button">
              <Icon name="close" size={24} />
            </button>
          }
          mobileMode="title"
          mobileTitle="Testando"
        />
      }
      maxWidth={700}
    >
      <div className="sq-speedtest-screen">
        <h1 className="overline sq-speedtest-screen__status" style={{ color: 'var(--accent)' }}>
          Teste em andamento
        </h1>

        <ProgressRing
          caption={
            <>
              <span className="sq-progress-ring__pulse" />
              <span className="body-small" style={{ color: 'var(--text-secondary)' }}>
                {status === 'running' ? 'medindo…' : progress?.message}
              </span>
            </>
          }
          phaseColor={PHASE_COLOR[phase]}
          phaseLabel={PHASE_LABEL[phase]}
          progress={PHASE_PROGRESS[phase]}
          unit=""
          value="--"
        />

        <StepTracker items={steps} />

        <span className="body-small" style={{ color: 'var(--text-secondary)' }}>
          Continue nesta aba até o fim — leva menos de um minuto.
        </span>
      </div>
    </AppShell>
  );
}
