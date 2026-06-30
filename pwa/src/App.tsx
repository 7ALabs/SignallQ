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
import { createDiagnosisWithAiFallback } from '@/features/diagnosis/aiClient';
import { DiagnosisResultPanel } from '@/features/diagnosis/components/DiagnosisResultPanel';
import { HistoryPanel } from '@/features/history/HistoryPanel';
import type { HistoryState } from '@/features/history/historyTypes';
import { getLocalReport } from '@/features/report/reportRepository';
import { ReportPage } from '@/features/report/ReportPage';
import type { Report } from '@/features/report/reportTypes';
import { SettingsPanel } from '@/features/settings/SettingsPanel';
import { runSpeedTestWeb } from '@/features/speedtest/speedTestRunner';
import type { SpeedTestProgress, SpeedTestRunStatus } from '@/features/speedtest/speedTestTypes';
import { InstallPromptBanner } from '@/shared/components/InstallPromptBanner';
import {
  type BeforeInstallPromptEvent,
  canShowInstallPromptBanner,
  getInstallEnvironment,
  listenForBeforeInstallPrompt,
  requestNativeInstallPrompt,
} from '@/shared/pwa/installPrompt';
import { historyRepository } from '@/shared/storage/historyRepository';
import { preferencesRepository } from '@/shared/storage/preferencesRepository';

const navItems = [
  { href: '#/', label: 'Início' },
  { href: '#/historico', label: 'Histórico' },
  { href: '#/ajustes', label: 'Ajustes' },
];

type AppRoute = { kind: 'history' } | { kind: 'home' } | { kind: 'report'; reportId: string } | { kind: 'settings' };

function readRoute(): AppRoute {
  const hash = window.location.hash.replace(/^#/, '');
  if (hash === '/historico') return { kind: 'history' };
  if (hash === '/ajustes') return { kind: 'settings' };
  const reportMatch = hash.match(/^\/laudo\/([^/?#]+)$/);
  if (reportMatch?.[1]) {
    return { kind: 'report', reportId: decodeURIComponent(reportMatch[1]) };
  }
  return { kind: 'home' };
}

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

function stabilityLabel(stability: DiagnosisResult['stability']): string {
  switch (stability) {
    case 'stable':
      return 'estável';
    case 'unstable':
      return 'instável';
    case 'unknown':
      return 'não medida';
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
  const [deferredInstallPrompt, setDeferredInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [diagnosis, setDiagnosis] = useState<DiagnosisResult | null>(null);
  const [diagnosisStatus, setDiagnosisStatus] = useState<'idle' | 'loading' | 'ready' | 'fallback' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [historyState, setHistoryState] = useState<HistoryState>({ entries: [], error: null, status: 'idle' });
  const [installPromptDismissed, setInstallPromptDismissed] = useState(
    () => preferencesRepository.getInstallPromptPreferences().dismissedAt != null,
  );
  const [installPromptIsEligible, setInstallPromptIsEligible] = useState(false);
  const [installPromptIsPrompting, setInstallPromptIsPrompting] = useState(false);
  const [progress, setProgress] = useState<SpeedTestProgress | null>(null);
  const [report, setReport] = useState<Report | null>(null);
  const [reportError, setReportError] = useState<string | null>(null);
  const [reportIsLoading, setReportIsLoading] = useState(false);
  const [route, setRoute] = useState<AppRoute>(() => readRoute());
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

  useEffect(() => {
    const timer = window.setTimeout(() => setInstallPromptIsEligible(true), 45_000);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => listenForBeforeInstallPrompt(setDeferredInstallPrompt), []);

  useEffect(() => {
    const handleHashChange = () => setRoute(readRoute());
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  useEffect(() => {
    if (route.kind !== 'report') {
      setReport(null);
      setReportError(null);
      setReportIsLoading(false);
      return;
    }

    let isCurrent = true;
    setReport(null);
    setReportError(null);
    setReportIsLoading(true);
    void getLocalReport(route.reportId)
      .then((localReport) => {
        if (isCurrent) setReport(localReport);
      })
      .catch((error) => {
        if (isCurrent) setReportError(error instanceof Error ? error.message : 'Falha ao abrir laudo local.');
      })
      .finally(() => {
        if (isCurrent) setReportIsLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [route]);

  const startTest = async () => {
    abortControllerRef.current?.abort();
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    setDiagnosis(null);
    setDiagnosisStatus('idle');
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

      setDiagnosisStatus('loading');
      const diagnosisOutcome = await createDiagnosisWithAiFallback(run.result);
      setDiagnosis(diagnosisOutcome.diagnosis);
      setDiagnosisStatus(diagnosisOutcome.source === 'ai' ? 'ready' : 'fallback');
      if (diagnosisOutcome.source === 'fallback') {
        setErrorMessage(`Análise avançada indisponível: ${diagnosisOutcome.errorMessage}`);
      }

      const entry: HistoryEntry = {
        createdAt: new Date().toISOString(),
        diagnosis: diagnosisOutcome.diagnosis,
        id: run.result.id,
        speedTest: run.result,
      };
      await historyRepository.save(entry);
      await refreshHistory();
      setInstallPromptIsEligible(true);
    } catch (error) {
      setStatus('error');
      setDiagnosisStatus('error');
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

  const copyCurrentReportLink = async () => {
    if (route.kind === 'report') await copyReportLink(route.reportId);
  };

  const openReport = (id: string) => {
    window.location.hash = `/laudo/${id}`;
  };

  const openHistory = () => {
    window.location.hash = '/historico';
  };

  const openSettings = () => {
    window.location.hash = '/ajustes';
  };

  const goHome = () => {
    window.location.hash = '/';
  };

  const dismissInstallPrompt = () => {
    preferencesRepository.dismissInstallPrompt();
    setInstallPromptDismissed(true);
  };

  const requestInstallPrompt = async () => {
    if (!deferredInstallPrompt) return;

    setInstallPromptIsPrompting(true);
    try {
      const outcome = await requestNativeInstallPrompt(deferredInstallPrompt);
      setDeferredInstallPrompt(null);
      if (outcome === 'accepted') dismissInstallPrompt();
    } finally {
      setInstallPromptIsPrompting(false);
    }
  };

  const isRunning = status === 'running';
  const installEnvironment = getInstallEnvironment({ hasNativePrompt: deferredInstallPrompt != null });
  const shouldShowInstallPrompt = canShowInstallPromptBanner({
    dismissed: installPromptDismissed,
    eligible: installPromptIsEligible,
    isDiagnosisLoading: diagnosisStatus === 'loading',
    isHomeRoute: route.kind === 'home',
    isRunning,
    isStandalone: installEnvironment.isStandalone,
  });
  const currentQuality = diagnosis?.quality ?? null;
  const downloadMbps = result?.download.mbps ?? null;
  const uploadMbps = result?.upload.mbps ?? null;
  const latencyMs = result?.latency.ms ?? null;
  const jitterMs = result?.jitter.ms ?? null;
  const perceivedLoss = result?.availability.perceivedLossPercent ?? null;
  const diagnosisActionDescription =
    diagnosisStatus === 'loading'
      ? 'Gerando análise avançada com fallback local preparado.'
      : diagnosisStatus === 'ready'
        ? 'Diagnóstico IA gerado a partir do payload web medido.'
        : diagnosisStatus === 'fallback'
          ? 'Diagnóstico local usado porque a análise avançada não respondeu.'
          : diagnosis
            ? 'Diagnóstico gerado a partir do resultado medido.'
            : 'Resumo simples e acionável após o teste.';

  if (route.kind === 'report') {
    return (
      <ThemeProvider mode="light">
        <AppShell
          header={
            <TopAppBar
              activeHref="#/historico"
              actions={<Button variant="text" onClick={goHome}>Voltar</Button>}
              navItems={navItems}
              subtitle="Laudo local"
              title="SignallQ"
            />
          }
        >
          <ReportPage
            error={reportError}
            isLoading={reportIsLoading}
            onBack={goHome}
            onCopyLink={copyCurrentReportLink}
            report={report}
            reportId={route.reportId}
          />
        </AppShell>
      </ThemeProvider>
    );
  }

  if (route.kind === 'history') {
    return (
      <ThemeProvider mode="light">
        <AppShell
          header={
            <TopAppBar
              activeHref="#/historico"
              actions={<Button variant="text" onClick={goHome}>Voltar</Button>}
              navItems={navItems}
              subtitle="Histórico local"
              title="SignallQ"
            />
          }
        >
          <HistoryPanel
            onClear={clearHistory}
            onCopyReportLink={copyReportLink}
            onOpenReport={openReport}
            onRemove={removeHistoryEntry}
            onStartTest={goHome}
            state={historyState}
          />
        </AppShell>
      </ThemeProvider>
    );
  }

  if (route.kind === 'settings') {
    return (
      <ThemeProvider mode="light">
        <AppShell
          header={
            <TopAppBar
              activeHref="#/ajustes"
              actions={<Button variant="text" onClick={goHome}>Voltar</Button>}
              navItems={navItems}
              subtitle="Privacidade e limites"
              title="SignallQ"
            />
          }
        >
          <SettingsPanel historyCount={historyState.entries.length} onClearHistory={clearHistory} onGoHome={goHome} />
        </AppShell>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider mode="light">
      <AppShell
        header={
          <TopAppBar
            activeHref="#/"
            actions={<Button variant="text" onClick={openSettings}>Privacidade</Button>}
            navItems={navItems}
            subtitle="Piloto M1"
            title="SignallQ"
          />
        }
      >
        {shouldShowInstallPrompt ? (
          <InstallPromptBanner
            environment={installEnvironment}
            isPrompting={installPromptIsPrompting}
            onDismiss={dismissInstallPrompt}
            onInstall={requestInstallPrompt}
          />
        ) : null}
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
              stabilityLabel={diagnosis ? `Estabilidade: ${stabilityLabel(diagnosis.stability)}` : 'Estabilidade por latência e jitter HTTP'}
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
                onClick={openHistory}
                title="Resultados anteriores"
              />
              <ActionCard
                description={diagnosisActionDescription}
                icon={<BrainCircuit size={22} />}
                meta="Diagnóstico"
                title="Análise da conexão"
              />
              <ActionCard
                description="Preferências web sem transformar a PWA em Android encapsulado."
                icon={<Settings size={22} />}
                meta="Ajustes"
                onClick={openSettings}
                title="Configuração da PWA"
              />
            </>
          }
          insights={
            <>
              <DiagnosisInsightCard
                body={
                  diagnosisStatus === 'loading'
                    ? 'A PWA está tentando a análise avançada. Se ela não responder, o diagnóstico local mantém o resultado utilizável.'
                    : errorMessage ??
                      'Quando uma métrica não puder ser medida no navegador, a interface deve mostrar essa limitação em vez de preencher valor falso.'
                }
                title="Sem métrica inventada"
                tone={errorMessage || diagnosisStatus === 'fallback' ? 'warning' : 'info'}
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
                onStartTest={goHome}
                state={historyState}
              />
            </>
          }
        />
      </AppShell>
    </ThemeProvider>
  );
}
