import { useCallback, useEffect, useRef, useState } from 'react';
import { Activity, BrainCircuit, Clock3, Gauge, History, RotateCcw, Settings } from 'lucide-react';
import type { DiagnosisResult, HistoryEntry, SpeedTestResult } from '@shared/contracts';
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
  ThemeProvider,
  TopAppBar,
} from '@/design-system';
import { DiagnosisResultPanel } from '@/features/diagnosis/components/DiagnosisResultPanel';
import { createLocalDiagnosis } from '@/features/diagnosis/localDiagnosis';
import { HistoryPanel } from '@/features/history/HistoryPanel';
import type { HistoryState } from '@/features/history/historyTypes';
import { runSpeedTestWeb } from '@/features/speedtest/speedTestRunner';
import type { SpeedTestProgress, SpeedTestRunStatus } from '@/features/speedtest/speedTestTypes';
import { historyRepository } from '@/shared/storage/historyRepository';

const navItems = ['Visão geral', 'Resultados', 'Ajustes'];

function formatMetric(value: number | null, maximumFractionDigits = 1): string {
  return value == null ? '--' : value.toLocaleString('pt-BR', { maximumFractionDigits });
}

function qualityLevel(quality: DiagnosisResult['quality']): 'good' | 'fair' | 'poor' | 'unknown' {
  switch (quality) {
    case 'good':
      return 'good';
    case 'attention':
      return 'fair';
    case 'bad':
      return 'poor';
    case 'unknown':
      return 'unknown';
  }
}

function qualityLabel(quality: DiagnosisResult['quality'] | null): string {
  switch (quality) {
    case 'good':
      return 'Conexão boa';
    case 'attention':
      return 'Atenção';
    case 'bad':
      return 'Conexão ruim';
    case 'unknown':
      return 'Inconclusivo';
    default:
      return 'Aguardando teste';
  }
}

function metricStatus(
  value: number | null,
  warningThreshold: number,
  criticalThreshold: number,
  inverse = false,
): 'good' | 'warning' | 'critical' | 'neutral' {
  if (value == null) return 'neutral';
  if (inverse) {
    if (value >= criticalThreshold) return 'critical';
    if (value >= warningThreshold) return 'warning';
    return 'good';
  }
  if (value <= criticalThreshold) return 'critical';
  if (value <= warningThreshold) return 'warning';
  return 'good';
}

function phaseLabel(progress: SpeedTestProgress | null, status: SpeedTestRunStatus): string {
  if (progress) return progress.message;
  if (status === 'idle') return 'Pronto para medir pelo navegador.';
  if (status === 'success') return 'Teste concluído.';
  if (status === 'partial') return 'Teste parcial concluído.';
  if (status === 'canceled') return 'Teste cancelado.';
  if (status === 'error') return 'Não foi possível medir a conexão.';
  return 'Medição em andamento.';
}

export function App() {
  const abortControllerRef = useRef<AbortController | null>(null);
  const [diagnosis, setDiagnosis] = useState<DiagnosisResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [historyState, setHistoryState] = useState<HistoryState>({ entries: [], error: null, status: 'idle' });
  const [progress, setProgress] = useState<SpeedTestProgress | null>(null);
  const [result, setResult] = useState<SpeedTestResult | null>(null);
  const [status, setStatus] = useState<SpeedTestRunStatus>('idle');

  const refreshHistory = useCallback(async () => {
    setHistoryState((current) => ({ ...current, error: null, status: 'loading' }));
    try {
      const entries = await historyRepository.list();
      setHistoryState({ entries, error: null, status: entries.length > 0 ? 'ready' : 'empty' });
    } catch (error) {
      setHistoryState({
        entries: [],
        error: error instanceof Error ? error.message : 'Falha ao ler histórico local.',
        status: 'error',
      });
    }
  }, []);

  useEffect(() => {
    void refreshHistory();
    return () => abortControllerRef.current?.abort();
  }, [refreshHistory]);

  const startTest = async () => {
    abortControllerRef.current?.abort();
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    setDiagnosis(null);
    setErrorMessage(null);
    setProgress(null);
    setResult(null);
    setStatus('running');

    try {
      const run = await runSpeedTestWeb({
        onProgress: setProgress,
        signal: abortController.signal,
      });

      setStatus(run.status);
      if (run.status === 'canceled') return;

      setResult(run.result);
      if (run.errorMessage) setErrorMessage('Não foi possível concluir todas as medições.');

      const localDiagnosis = createLocalDiagnosis({ speedTest: run.result });
      setDiagnosis(localDiagnosis);

      const entry: HistoryEntry = {
        createdAt: new Date().toISOString(),
        diagnosis: localDiagnosis,
        id: run.result.id,
        speedTest: run.result,
      };
      await historyRepository.save(entry);
      await refreshHistory();
    } catch (error) {
      setStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'Falha inesperada durante o teste.');
    } finally {
      if (abortControllerRef.current === abortController) {
        abortControllerRef.current = null;
      }
    }
  };

  const cancelTest = () => {
    abortControllerRef.current?.abort();
  };

  const removeHistoryEntry = async (id: string) => {
    await historyRepository.remove(id);
    await refreshHistory();
  };

  const clearHistory = async () => {
    await historyRepository.clear();
    await refreshHistory();
  };

  const copyReportLink = async (id: string) => {
    const link = `${window.location.origin}${window.location.pathname}#/laudo/${id}`;
    await navigator.clipboard?.writeText(link);
  };

  const openReport = (id: string) => {
    window.location.hash = `/laudo/${id}`;
  };

  const isRunning = status === 'running';
  const currentQuality = diagnosis?.quality ?? null;
  const downloadMbps = result?.download.mbps ?? null;
  const uploadMbps = result?.upload.mbps ?? null;
  const latencyMs = result?.latency.ms ?? null;
  const jitterMs = result?.jitter.ms ?? null;
  const perceivedLoss = result?.availability.perceivedLossPercent ?? null;

  return (
    <ThemeProvider mode="light">
      <AppShell
        header={
          <TopAppBar
            actions={<Button variant="text">Ajuda</Button>}
            navItems={navItems}
            subtitle="Piloto M1"
            title="SignallQ"
          />
        }
      >
        <HomeLayout
          hero={
            <SpeedHeroCard
              action={
                <div className="sq-speed-actions">
                  <Button icon={<Gauge size={18} />} isLoading={isRunning} onClick={startTest}>
                    Iniciar teste
                  </Button>
                  {isRunning ? (
                    <Button icon={<RotateCcw size={18} />} variant="tonal" onClick={cancelTest}>
                      Cancelar
                    </Button>
                  ) : null}
                </div>
              }
              caption={phaseLabel(progress, status)}
              downloadLabel={result ? 'Download medido via HTTP' : 'Download ainda não medido'}
              qualityLabel={qualityLabel(currentQuality)}
              stabilityLabel={diagnosis ? `Estabilidade: ${diagnosis.stability}` : 'Estabilidade por latência e jitter HTTP'}
              title="Meça velocidade e estabilidade sem inventar sinal nativo"
              value={formatMetric(downloadMbps)}
            />
          }
          summary={
            <ConnectionSummaryCard
              description={
                diagnosis?.summary ??
                'O teste usa endpoints HTTP controlados para download, upload, latência e jitter aproximado. Ping ICMP, RSSI e scan Wi-Fi não existem no browser.'
              }
              quality={currentQuality ? qualityLevel(currentQuality) : 'unknown'}
              qualityLabel={qualityLabel(currentQuality)}
              title={result ? 'Resultado do teste web' : 'Pronto para medir neste navegador'}
            />
          }
          metrics={
            <>
              <MetricTile
                helperText={
                  result?.download.status === 'measured'
                    ? `${result.download.bytes.toLocaleString('pt-BR')} bytes recebidos.`
                    : 'Medição HTTP controlada.'
                }
                icon={<Gauge size={22} />}
                label="Download"
                status={metricStatus(downloadMbps, 10, 3)}
                unit="Mbps"
                value={formatMetric(downloadMbps)}
              />
              <MetricTile
                helperText={
                  result?.upload.status === 'not_available' ? 'Endpoint de upload indisponível.' : 'POST para endpoint controlado.'
                }
                icon={<Activity size={22} />}
                label="Upload"
                status={metricStatus(uploadMbps, 5, 1)}
                unit="Mbps"
                value={formatMetric(uploadMbps)}
              />
              <MetricTile
                helperText="Aproximação via fetch/timing do navegador."
                icon={<Clock3 size={22} />}
                label="Latência HTTP"
                status={metricStatus(latencyMs, 80, 150, true)}
                unit="ms"
                value={formatMetric(latencyMs, 0)}
              />
              <MetricTile
                helperText="Variação entre amostras de latência HTTP."
                icon={<Activity size={22} />}
                label="Jitter"
                status={metricStatus(jitterMs, 20, 40, true)}
                unit="ms"
                value={formatMetric(jitterMs, 0)}
              />
              <MetricTile
                helperText="Inferência por falhas/timeouts HTTP, não perda real de pacote."
                icon={<Activity size={22} />}
                label="Falhas percebidas"
                status={metricStatus(perceivedLoss, 5, 20, true)}
                unit="%"
                value={formatMetric(perceivedLoss)}
              />
            </>
          }
          actions={
            <>
              <ActionCard
                description={`${historyState.entries.length} medição(ões) salvas neste navegador.`}
                icon={<History size={22} />}
                meta="Histórico"
                title="Resultados anteriores"
              />
              <ActionCard
                description={diagnosis ? 'Diagnóstico local gerado a partir do resultado medido.' : 'Resumo simples e acionável após o teste.'}
                icon={<BrainCircuit size={22} />}
                meta="Diagnóstico"
                title="Análise da conexão"
              />
              <ActionCard
                description="Preferências web sem transformar a PWA em Android encapsulado."
                icon={<Settings size={22} />}
                meta="Ajustes"
                title="Configuração da PWA"
              />
            </>
          }
          insights={
            <>
              <DiagnosisInsightCard
                body={
                  errorMessage ??
                  'Quando uma métrica não puder ser medida no navegador, a interface deve mostrar essa limitação em vez de preencher valor falso.'
                }
                title="Sem métrica inventada"
                tone={errorMessage ? 'warning' : 'info'}
              />
              <div className="sq-diagnosis-layout">
                <DiagnosisResultPanel diagnosis={diagnosis} />
                <NetworkContextCard
                  items={[
                    { label: 'Tipo de conexão', value: result?.connection.effectiveType ?? 'Quando disponível' },
                    { label: 'Wi-Fi detalhado', value: 'Indisponível na web' },
                    { label: 'Amostras de latência', value: result ? String(result.latency.samples) : 'Aguardando teste' },
                  ]}
                  title="Contexto do navegador"
                />
                <RecommendationList
                  items={
                    diagnosis?.actions.slice(0, 3).map((action) => action.description) ?? [
                      'Comece pelo teste principal antes de abrir detalhes.',
                      'Leia velocidade e estabilidade como sinais separados.',
                      'Use diagnóstico curto, claro e sem jargão desnecessário.',
                    ]
                  }
                  title="Ações recomendadas"
                />
              </div>
              <HistoryPanel
                onClear={clearHistory}
                onCopyReportLink={copyReportLink}
                onOpenReport={openReport}
                onRemove={removeHistoryEntry}
                state={historyState}
              />
            </>
          }
        />
      </AppShell>
    </ThemeProvider>
  );
}
