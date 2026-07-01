import { Activity, BrainCircuit, Clock3, Gauge, History, RotateCcw, Wifi, XCircle } from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { DiagnosisResult, SpeedTestResult } from '@shared/contracts';
import {
  ActionCard,
  AppShell,
  Button,
  ConnectionSummaryCard,
  DiagnosisInsightCard,
  HomeLayout,
  MetricTile,
  NetworkContextCard,
  RecommendationList,
  SpeedHeroCard,
  TopAppBar,
} from '@/design-system';
import { createDiagnosisWithAiFallback } from '@/features/diagnosis/aiClient';
import { DiagnosisResultPanel } from '@/features/diagnosis/components/DiagnosisResultPanel';
import { createLocalDiagnosis } from '@/features/diagnosis/localDiagnosis';
import { HistoryPanel } from '@/features/history/HistoryPanel';
import type { HistoryState } from '@/features/history/historyTypes';
import { buildHistoryState, createHistoryEntry, historyErrorMessage } from '@/features/history/historyViewModel';
import { useConnectionSnapshot } from '@/hooks/useConnectionSnapshot';
import { historyRepository } from '@/shared/storage/historyRepository';
import { ConnectionStatus, SpeedtestPhase } from '@/types/network';
import { classifySpeedTest, type OverallQuality, type SpeedTestQuality } from './qualityClassifier';
import { runSpeedTestWeb } from './speedTestRunner';
import type { SpeedTestProgress, SpeedTestRunStatus } from './speedTestTypes';

const navItems = ['Teste', 'Histórico', 'Limitações'];

type DiagnosisStatus = 'idle' | 'local' | 'loading-ai' | 'ai' | 'fallback';

interface SpeedTestPageProps {
  onNavigateHistory: () => void;
  onOpenReport: (id: string) => void;
}

const phaseLabels: Record<SpeedtestPhase, string> = {
  [SpeedtestPhase.Idle]: 'Aguardando',
  [SpeedtestPhase.Latency]: 'Medindo latência HTTP',
  [SpeedtestPhase.Download]: 'Medindo download',
  [SpeedtestPhase.Upload]: 'Medindo upload',
  [SpeedtestPhase.Complete]: 'Concluído',
  [SpeedtestPhase.Partial]: 'Resultado parcial',
  [SpeedtestPhase.Error]: 'Erro',
  [SpeedtestPhase.Canceled]: 'Cancelado',
};

function formatNumber(value: number | null, maximumFractionDigits = 1): string {
  return value == null ? '--' : value.toLocaleString('pt-BR', { maximumFractionDigits });
}

function metricStatus(value: number | null, quality: 'good' | 'warning' | 'critical' | 'neutral'): 'good' | 'warning' | 'critical' | 'neutral' {
  return value == null ? 'neutral' : quality;
}

function summaryQuality(overall: OverallQuality): 'good' | 'fair' | 'poor' | 'unknown' {
  if (overall === 'good') return 'good';
  if (overall === 'attention') return 'fair';
  if (overall === 'bad') return 'poor';
  return 'unknown';
}

function qualityLabel(overall: OverallQuality): string {
  if (overall === 'good') return 'Boa';
  if (overall === 'attention') return 'Atenção';
  if (overall === 'bad') return 'Ruim';
  return 'Sem teste';
}

function runStatusLabel(status: SpeedTestRunStatus): string {
  if (status === 'idle') return 'Pronto';
  if (status === 'running') return 'Medindo';
  if (status === 'partial') return 'Parcial';
  if (status === 'success') return 'Concluído';
  if (status === 'error') return 'Erro';
  return 'Cancelado';
}

function statusCaption(status: SpeedTestRunStatus, progress: SpeedTestProgress): string {
  if (status === 'running') return progress.message;
  if (status === 'partial') return 'Teste parcial concluído. Métricas indisponíveis aparecem sem valor inventado.';
  if (status === 'success') return 'Teste concluído com download, upload, latência e estabilidade.';
  if (status === 'error') return 'Não foi possível concluir a medição. Tente novamente.';
  if (status === 'canceled') return 'Teste cancelado antes da conclusão.';
  return 'Pronto para medir sua conexão pelo navegador.';
}

function limitationText(code: string): string {
  const labels: Record<string, string> = {
    browser_measurement_may_vary: 'Medição sujeita a políticas do navegador, cache e servidor.',
    http_latency_not_icmp_ping: 'Latência medida por requisições HTTP, não ping ICMP real.',
    network_information_api_unavailable: 'Tipo de conexão indisponível neste navegador.',
    packet_loss_not_directly_measured: 'Perda é inferida por falhas HTTP, não medida por pacote.',
    upload_endpoint_unavailable: 'Upload não foi medido porque não havia endpoint disponível.',
    wifi_signal_not_available_on_web: 'Sinal Wi-Fi real não está disponível no navegador.',
  };

  return labels[code] ?? code;
}

export function SpeedTestPage({ onNavigateHistory, onOpenReport }: SpeedTestPageProps) {
  const connection = useConnectionSnapshot();
  const abortRef = useRef<AbortController | null>(null);
  const [progress, setProgress] = useState<SpeedTestProgress>({
    phase: SpeedtestPhase.Idle,
    status: 'idle',
    message: 'Pronto para medir sua conexão pelo navegador.',
  });
  const [result, setResult] = useState<SpeedTestResult | null>(null);
  const [diagnosis, setDiagnosis] = useState<DiagnosisResult | null>(null);
  const [diagnosisStatus, setDiagnosisStatus] = useState<DiagnosisStatus>('idle');
  const [history, setHistory] = useState<HistoryState>(() => buildHistoryState([], 'loading'));

  const quality = useMemo<SpeedTestQuality>(() => classifySpeedTest(result), [result]);
  const isRunning = progress.status === 'running';

  const loadHistory = useCallback(async () => {
    setHistory((current) => buildHistoryState(current.entries, 'loading'));
    try {
      const entries = await historyRepository.list();
      setHistory(buildHistoryState(entries, entries.length > 0 ? 'ready' : 'empty'));
    } catch (error) {
      setHistory(buildHistoryState([], 'error', historyErrorMessage(error, 'Falha ao ler histórico local.')));
    }
  }, []);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  useEffect(() => {
    return () => abortRef.current?.abort();
  }, []);

  const saveResult = useCallback(
    async (speedTest: SpeedTestResult, nextDiagnosis: DiagnosisResult) => {
      try {
        await historyRepository.save(createHistoryEntry(speedTest, nextDiagnosis));
        await loadHistory();
      } catch (error) {
        setHistory((current) => buildHistoryState(current.entries, 'error', historyErrorMessage(error, 'Falha ao salvar histórico local.')));
      }
    },
    [loadHistory],
  );

  const startTest = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setProgress({
      phase: SpeedtestPhase.Latency,
      status: 'running',
      message: 'Iniciando medição HTTP...',
    });

    try {
      const run = await runSpeedTestWeb({
        onProgress: setProgress,
        signal: controller.signal,
      });

      if (run.status === 'canceled') {
        setProgress({
          phase: SpeedtestPhase.Canceled,
          status: 'canceled',
          message: 'Teste cancelado.',
        });
        return;
      }

      setResult(run.result);
      const localDiagnosis = createLocalDiagnosis({ speedTest: run.result });
      setDiagnosis(localDiagnosis);
      setDiagnosisStatus('loading-ai');

      const ai = await createDiagnosisWithAiFallback(run.result);
      setDiagnosis(ai.diagnosis);
      setDiagnosisStatus(ai.source === 'ai' ? 'ai' : 'fallback');
      await saveResult(run.result, ai.diagnosis);
    } catch {
      setProgress({
        phase: SpeedtestPhase.Error,
        status: 'error',
        message: 'Não foi possível medir agora.',
      });
    } finally {
      if (abortRef.current === controller) abortRef.current = null;
    }
  }, [saveResult]);

  const cancelTest = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  const clearHistory = useCallback(async () => {
    try {
      await historyRepository.clear();
      setHistory(buildHistoryState([], 'empty'));
    } catch (error) {
      setHistory((current) => buildHistoryState(current.entries, 'error', historyErrorMessage(error, 'Falha ao limpar histórico local.')));
    }
  }, []);

  const removeHistoryEntry = useCallback(async (id: string) => {
    try {
      await historyRepository.remove(id);
      const entries = await historyRepository.list();
      setHistory(buildHistoryState(entries, entries.length > 0 ? 'ready' : 'empty'));
    } catch (error) {
      setHistory((current) => buildHistoryState(current.entries, 'error', historyErrorMessage(error, 'Falha ao remover item do histórico.')));
    }
  }, []);

  const openHistoryEntry = useCallback(
    (id: string) => {
      const entry = history.entries.find((item) => item.id === id);
      if (!entry) return;
      setResult(entry.speedTest);
      setDiagnosis(entry.diagnosis);
      setDiagnosisStatus(entry.diagnosis.source === 'ai' ? 'ai' : entry.diagnosis.source === 'fallback' ? 'fallback' : 'local');
      setProgress({
        phase: SpeedtestPhase.Complete,
        status: 'success',
        message: 'Resultado salvo carregado.',
      });
    },
    [history.entries],
  );

  const copyReportLink = useCallback(async (id: string) => {
    const url = `${window.location.origin}/laudo/${id}`;
    await navigator.clipboard?.writeText(url);
  }, []);

  return (
    <AppShell
      header={
        <TopAppBar
          actions={<Button variant="text" onClick={() => void loadHistory()}>Atualizar</Button>}
          navItems={[
            { href: '/', label: navItems[0] ?? 'Teste' },
            { href: '/historico', label: navItems[1] ?? 'Histórico' },
            { href: '#limitacoes', label: navItems[2] ?? 'Limitações' },
          ]}
          subtitle="M1 SpeedTest web"
          title="SignallQ"
        />
      }
    >
      <HomeLayout
        hero={
          <SpeedHeroCard
            action={
              isRunning ? (
                <Button icon={<XCircle size={18} />} variant="secondary" onClick={cancelTest}>
                  Cancelar
                </Button>
              ) : (
                <Button icon={<Gauge size={18} />} onClick={() => void startTest()}>
                  {result ? 'Testar novamente' : 'Iniciar teste'}
                </Button>
              )
            }
            caption={statusCaption(progress.status, progress)}
            downloadLabel={result?.download.status === 'measured' ? 'Mbps de download' : 'Download não medido'}
            qualityLabel={qualityLabel(quality.overall)}
            stabilityLabel={`${phaseLabels[progress.phase]} / ${progress.status}`}
            title="Meça velocidade e estabilidade pelo navegador"
            value={formatNumber(result?.download.mbps ?? null)}
          />
        }
        summary={
          <ConnectionSummaryCard
            description={quality.summary}
            quality={summaryQuality(quality.overall)}
            qualityLabel={qualityLabel(quality.overall)}
            title={result ? 'Resultado do teste web' : 'Pronto para medir sua conexão'}
          />
        }
        metrics={
          <>
            <MetricTile
              helperText="Medição HTTP controlada, não ICMP ping."
              icon={<Gauge size={22} />}
              label="Download"
              status={metricStatus(result?.download.mbps ?? null, quality.speed === 'slow' ? 'critical' : 'good')}
              unit="Mbps"
              value={formatNumber(result?.download.mbps ?? null)}
            />
            <MetricTile
              helperText={result?.upload.status === 'not_available' ? 'Endpoint de upload indisponível.' : 'Upload via endpoint controlado.'}
              icon={<Activity size={22} />}
              label="Upload"
              status={metricStatus(result?.upload.mbps ?? null, 'good')}
              unit="Mbps"
              value={formatNumber(result?.upload.mbps ?? null)}
            />
            <MetricTile
              helperText="Latência aproximada por fetch/timing."
              icon={<Clock3 size={22} />}
              label="Latência"
              status={metricStatus(result?.latency.ms ?? null, quality.stability === 'unstable' ? 'critical' : 'good')}
              unit="ms"
              value={formatNumber(result?.latency.ms ?? null, 0)}
            />
          </>
        }
        actions={
          <>
            <ActionCard
              description={
                diagnosisStatus === 'loading-ai'
                  ? 'Diagnóstico local pronto. Analisando com IA pelo Worker, sem bloquear o resultado.'
                  : diagnosisStatus === 'fallback'
                    ? 'A IA não respondeu neste ambiente. O diagnóstico local foi mantido e salvo.'
                    : `${phaseLabels[progress.phase]}: ${progress.message}`
              }
              icon={diagnosisStatus === 'loading-ai' ? <BrainCircuit size={22} /> : isRunning ? <RotateCcw size={22} /> : <Gauge size={22} />}
              meta={diagnosisStatus === 'loading-ai' || diagnosisStatus === 'ai' || diagnosisStatus === 'fallback' ? 'Diagnóstico IA' : 'Progresso'}
              title={
                diagnosisStatus === 'loading-ai'
                  ? 'Analisando'
                  : diagnosisStatus === 'ai'
                    ? 'IA aplicada'
                    : diagnosisStatus === 'fallback'
                      ? 'Fallback local'
                      : runStatusLabel(progress.status)
              }
            />
            <ActionCard
              description={`${history.entries.length} resultado(s) salvo(s) neste navegador.`}
              icon={<History size={22} />}
              meta="IndexedDB"
              title="Histórico local"
              action={<Button variant="text" onClick={onNavigateHistory}>Abrir</Button>}
            />
            <ActionCard
              description={connection.status === ConnectionStatus.Online ? 'Navegador online para medir agora.' : 'Navegador offline. Resultados salvos continuam locais.'}
              icon={<Wifi size={22} />}
              meta="Rede"
              title={connection.status}
            />
          </>
        }
        insights={
          <>
            <div className="sq-speedtest-stack">
              <DiagnosisResultPanel diagnosis={diagnosis} />
              <HistoryPanel
                onClear={clearHistory}
                onCopyReportLink={(id) => void copyReportLink(id)}
                onOpenReport={(id) => {
                  openHistoryEntry(id);
                  onOpenReport(id);
                }}
                onRemove={(id) => void removeHistoryEntry(id)}
                state={history}
              />
            </div>
            <div className="sq-diagnosis-layout">
              <NetworkContextCard
                items={[
                  { label: 'Status', value: connection.status },
                  { label: 'Tipo estimado', value: connection.effectiveType ?? 'Indisponível' },
                  { label: 'Downlink API', value: connection.downlinkMbps == null ? 'Indisponível' : `${connection.downlinkMbps} Mbps` },
                ]}
                title="Contexto do navegador"
              />
              <RecommendationList
                items={[
                  quality.usage.streaming === 'unknown' ? 'Execute um teste para avaliar streaming.' : `Streaming: ${quality.usage.streaming}`,
                  quality.usage.videoCall === 'unknown' ? 'Execute um teste para avaliar videochamada.' : `Videochamada: ${quality.usage.videoCall}`,
                  quality.usage.gaming === 'unknown' ? 'Execute um teste para avaliar jogos.' : `Jogos: ${quality.usage.gaming}`,
                ]}
                title="Vereditos de uso"
              />
              <DiagnosisInsightCard
                body={(result?.limitations ?? ['http_latency_not_icmp_ping', 'packet_loss_not_directly_measured'])
                  .map(limitationText)
                  .join(' ')}
                eyebrow="Limitações"
                id="limitacoes"
                title="Medição honesta no browser"
              />
            </div>
          </>
        }
      />
    </AppShell>
  );
}
