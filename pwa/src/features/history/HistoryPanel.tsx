import { useMemo } from 'react';
import { AppShell, Button, HistoryTable, Icon, TopAppBar } from '@/design-system';
import type { HistoryTableRow } from '@/design-system';
import { qualityLabel, qualityLevelFromQuality, stabilityLabel } from '@/shared/verdict';
import type { HistoryState } from './historyTypes';

interface HistoryPanelProps {
  onBack: () => void;
  onClear: () => void;
  onOpenEntry: (id: string) => void;
  onStartTest?: () => void;
  state: HistoryState;
}

const NAV_ITEMS = [
  { href: '#/home', label: 'Início' },
  { href: '#/historico', label: 'Histórico' },
  { href: '#/ajustes', label: 'Ajustes' },
  { href: '#/sobre', label: 'Sobre' },
];

export function HistoryPanel({ onBack, onClear, onOpenEntry, onStartTest, state }: HistoryPanelProps) {
  const rows: HistoryTableRow[] = useMemo(
    () =>
      state.entries.map((entry) => ({
        dateLabel: new Date(entry.createdAt).toLocaleString('pt-BR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }),
        downloadLabel: entry.speedTest.download.mbps != null ? `${entry.speedTest.download.mbps.toFixed(0)} Mbps` : '--',
        id: entry.id,
        latencyLabel: entry.speedTest.latency.ms != null ? `${entry.speedTest.latency.ms} ms` : '--',
        qualityLabel: qualityLabel(entry.diagnosis.quality),
        qualityLevel: qualityLevelFromQuality(entry.diagnosis.quality),
        stabilityLabel: stabilityLabel(entry.diagnosis.stability),
      })),
    [state.entries],
  );

  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <Button disabled={rows.length === 0} icon={<Icon name="delete_sweep" size={16} />} onClick={onClear} variant="danger-outline">
              Limpar histórico
            </Button>
          }
          activeHref="#/historico"
          mobileAction={
            <button aria-label="Limpar histórico" className="sq-icon-button" disabled={rows.length === 0} onClick={onClear} type="button">
              <Icon name="delete_sweep" size={19} style={{ color: 'var(--error)' }} />
            </button>
          }
          mobileMode="back"
          mobileTitle="Histórico"
          navItems={NAV_ITEMS}
          onMobileBack={onBack}
        />
      }
      maxWidth={920}
    >
      <section aria-label="Histórico local" className="sq-history-screen">
        <h1 className="sq-visually-hidden">Histórico</h1>

        {state.status === 'loading' ? <p aria-live="polite" className="sq-history-panel__message">Carregando histórico local...</p> : null}

        {state.status === 'error' ? (
          <div className="sq-history-panel__message sq-history-panel__message--error" role="alert">
            <strong>Histórico indisponível</strong>
            <p>{state.error}</p>
          </div>
        ) : null}

        {state.status === 'empty' || (state.status === 'idle' && state.entries.length === 0) ? (
          <div className="sq-history-panel__message">
            <strong>Nenhuma medição salva ainda</strong>
            <p>Faça um teste para criar o primeiro laudo local neste navegador.</p>
            {onStartTest ? (
              <Button onClick={onStartTest} variant="tonal">
                Iniciar teste
              </Button>
            ) : null}
          </div>
        ) : null}

        {rows.length > 0 ? (
          <>
            <h2 className="sq-history-screen__title">
              {rows.length} {rows.length === 1 ? 'teste salvo' : 'testes salvos'}
            </h2>
            <HistoryTable onOpen={onOpenEntry} rows={rows} />
          </>
        ) : null}
      </section>
    </AppShell>
  );
}
