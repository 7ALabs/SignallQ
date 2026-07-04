import { useCallback, useEffect, useRef, useState } from 'react';
import type { DiagnosisResult, HistoryEntry, SpeedTestResult } from '@shared/contracts';
import { ThemeProvider } from '@/design-system';
import { buildAdminDiagnosticPayload } from '@/features/diagnosis/adminIngestPayload';
import { createDiagnosisWithAiFallback } from '@/features/diagnosis/aiClient';
import { AboutScreen } from '@/features/about/AboutScreen';
import { HistoryPanel } from '@/features/history/HistoryPanel';
import type { HistoryState } from '@/features/history/historyTypes';
import { TestDetailScreen } from '@/features/history/TestDetailScreen';
import { HomeScreen } from '@/features/home/HomeScreen';
import type { HomeScreenLatestResult } from '@/features/home/HomeScreen';
import { LandingScreen } from '@/features/landing/LandingScreen';
import { getLocalReport } from '@/features/report/reportRepository';
import { ReportPage } from '@/features/report/ReportPage';
import type { Report } from '@/features/report/reportTypes';
import { ResultScreen } from '@/features/result/ResultScreen';
import { SettingsPanel } from '@/features/settings/SettingsPanel';
import { runSpeedTestWeb } from '@/features/speedtest/speedTestRunner';
import { SpeedTestScreen } from '@/features/speedtest/SpeedTestScreen';
import type { SpeedTestProgress, SpeedTestRunStatus } from '@/features/speedtest/speedTestTypes';
import { sendAdminDiagnostic } from '@/services/api';
import { InstallPromptBanner } from '@/shared/components/InstallPromptBanner';
import {
  type BeforeInstallPromptEvent,
  canShowInstallPromptBanner,
  getInstallEnvironment,
  listenForBeforeInstallPrompt,
  requestNativeInstallPrompt,
} from '@/shared/pwa/installPrompt';
import { historyRepository } from '@/shared/storage/historyRepository';
import { preferencesRepository, type ThemePreference } from '@/shared/storage/preferencesRepository';

export type AppRoute =
  | { kind: 'landing' }
  | { kind: 'home' }
  | { kind: 'speedtest' }
  | { kind: 'result' }
  | { kind: 'history' }
  | { kind: 'testDetail'; entryId: string }
  | { kind: 'settings' }
  | { kind: 'about' }
  | { kind: 'report'; reportId: string };

export function readRoute(): AppRoute {
  const hash = window.location.hash.replace(/^#/, '');
  if (hash === '/home') return { kind: 'home' };
  if (hash === '/teste') return { kind: 'speedtest' };
  if (hash === '/resultado') return { kind: 'result' };
  if (hash === '/historico') return { kind: 'history' };
  if (hash === '/ajustes') return { kind: 'settings' };
  if (hash === '/sobre') return { kind: 'about' };
  const testDetailMatch = hash.match(/^\/teste\/([^/?#]+)$/);
  if (testDetailMatch?.[1]) {
    return { kind: 'testDetail', entryId: decodeURIComponent(testDetailMatch[1]) };
  }
  const reportMatch = hash.match(/^\/laudo\/([^/?#]+)$/);
  if (reportMatch?.[1]) {
    return { kind: 'report', reportId: decodeURIComponent(reportMatch[1]) };
  }
  return { kind: 'landing' };
}

/** Reabertura do PWA instalado: usuário com histórico salvo não deve cair no onboarding (landing). */
export function shouldRedirectRecurringUserToHome(params: {
  hash: string;
  historyStatus: HistoryState['status'];
  historyEntryCount: number;
}): boolean {
  const hasExplicitHash = params.hash.replace(/^#/, '') !== '';
  return !hasExplicitHash && params.historyStatus === 'ready' && params.historyEntryCount > 0;
}

export function App() {
  const abortControllerRef = useRef<AbortController | null>(null);
  const [deferredInstallPrompt, setDeferredInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [diagnosis, setDiagnosis] = useState<DiagnosisResult | null>(null);
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
  const [themeMode, setThemeModeState] = useState<ThemePreference>(() => preferencesRepository.getThemePreference());

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
    const shouldRedirect = shouldRedirectRecurringUserToHome({
      hash: window.location.hash,
      historyEntryCount: historyState.entries.length,
      historyStatus: historyState.status,
    });
    if (shouldRedirect) {
      window.location.hash = '/home';
    }
  }, [historyState]);

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

  useEffect(() => {
    if (route.kind === 'result' && !result) {
      window.location.hash = '/home';
      return;
    }
    if (route.kind === 'testDetail' && historyState.status !== 'loading' && !historyState.entries.some((entry) => entry.id === route.entryId)) {
      window.location.hash = '/historico';
    }
  }, [route, result, historyState]);

  const goLanding = () => {
    window.location.hash = '/';
  };

  const goHome = () => {
    window.location.hash = '/home';
  };

  const openHistory = () => {
    window.location.hash = '/historico';
  };

  const openSettings = () => {
    window.location.hash = '/ajustes';
  };

  const goAbout = () => {
    window.location.hash = '/sobre';
  };

  const openTestDetail = (id: string) => {
    window.location.hash = `/teste/${encodeURIComponent(id)}`;
  };

  const startTest = async () => {
    abortControllerRef.current?.abort();
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    setDiagnosis(null);
    setErrorMessage(null);
    setProgress(null);
    setResult(null);
    setStatus('running');
    window.location.hash = '/teste';

    try {
      const run = await runSpeedTestWeb({
        onProgress: setProgress,
        signal: abortController.signal,
      });

      setStatus(run.status);
      if (run.status === 'canceled') {
        goHome();
        return;
      }

      setResult(run.result);
      if (run.errorMessage) setErrorMessage('Não foi possível concluir todas as medições.');

      const diagnosisOutcome = await createDiagnosisWithAiFallback(run.result);
      setDiagnosis(diagnosisOutcome.diagnosis);
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

      // GH#441: fire-and-forget — nao bloqueia a navegacao nem falha o teste
      // se o Console estiver fora do ar (mesma postura fire-and-forget do Android).
      void sendAdminDiagnostic(buildAdminDiagnosticPayload(run.result, diagnosisOutcome.diagnosis));

      setInstallPromptIsEligible(true);
      window.location.hash = '/resultado';
    } catch (error) {
      setStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'Falha inesperada durante o teste.');
      goHome();
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

  const setThemeMode = (mode: ThemePreference) => {
    setThemeModeState(mode);
    preferencesRepository.setThemePreference(mode);
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

  const installEnvironment = getInstallEnvironment({ hasNativePrompt: deferredInstallPrompt != null });
  const shouldShowInstallPrompt = canShowInstallPromptBanner({
    dismissed: installPromptDismissed,
    eligible: installPromptIsEligible,
    isDiagnosisLoading: false,
    isHomeRoute: route.kind === 'home',
    isRunning: status === 'running',
    isStandalone: installEnvironment.isStandalone,
  });

  const installBanner = shouldShowInstallPrompt ? (
    <InstallPromptBanner
      environment={installEnvironment}
      isPrompting={installPromptIsPrompting}
      onDismiss={dismissInstallPrompt}
      onInstall={requestInstallPrompt}
    />
  ) : null;

  if (route.kind === 'landing') {
    return (
      <ThemeProvider mode={themeMode}>
        <LandingScreen onOpenAbout={goAbout} onStartTest={startTest} />
      </ThemeProvider>
    );
  }

  if (route.kind === 'speedtest') {
    return (
      <ThemeProvider mode={themeMode}>
        <SpeedTestScreen onCancel={cancelTest} progress={progress} status={status} />
      </ThemeProvider>
    );
  }

  if (route.kind === 'result') {
    if (!result) return null;
    return (
      <ThemeProvider mode={themeMode}>
        <ResultScreen diagnosis={diagnosis} onCopyLink={() => void copyReportLink(result.id)} onRetry={startTest} result={result} />
      </ThemeProvider>
    );
  }

  if (route.kind === 'history') {
    return (
      <ThemeProvider mode={themeMode}>
        {installBanner}
        <HistoryPanel onBack={goHome} onClear={clearHistory} onOpenEntry={openTestDetail} onStartTest={startTest} state={historyState} />
      </ThemeProvider>
    );
  }

  if (route.kind === 'testDetail') {
    const entry = historyState.entries.find((item) => item.id === route.entryId);
    if (entry) {
      return (
        <ThemeProvider mode={themeMode}>
          <TestDetailScreen
            entry={entry}
            onBack={openHistory}
            onRemove={() => void removeHistoryEntry(entry.id).then(openHistory)}
            onRetry={startTest}
          />
        </ThemeProvider>
      );
    }
    return null;
  }

  if (route.kind === 'settings') {
    return (
      <ThemeProvider mode={themeMode}>
        <SettingsPanel
          historyCount={historyState.entries.length}
          onBack={goHome}
          onClearHistory={clearHistory}
          onOpenAbout={goAbout}
          setThemeMode={setThemeMode}
          themeMode={themeMode}
        />
      </ThemeProvider>
    );
  }

  if (route.kind === 'about') {
    return (
      <ThemeProvider mode={themeMode}>
        <AboutScreen onBack={goHome} />
      </ThemeProvider>
    );
  }

  if (route.kind === 'report') {
    return (
      <ThemeProvider mode={themeMode}>
        <ReportPage
          error={reportError}
          isLoading={reportIsLoading}
          onBack={goHome}
          onCopyLink={copyCurrentReportLink}
          report={report}
          reportId={route.reportId}
        />
      </ThemeProvider>
    );
  }

  const latest: HomeScreenLatestResult | null = (() => {
    const source = result && diagnosis ? { createdAt: new Date().toISOString(), diagnosis, speedTest: result } : historyState.entries[0];
    if (!source) return null;
    const level = source.diagnosis.quality === 'good' ? 'good' : source.diagnosis.quality === 'attention' ? 'fair' : source.diagnosis.quality === 'bad' ? 'poor' : 'unknown';
    const label = source.diagnosis.quality === 'good' ? 'Bom' : source.diagnosis.quality === 'attention' ? 'Atenção' : source.diagnosis.quality === 'bad' ? 'Ruim' : 'Inconclusivo';
    return {
      dateLabel: new Date(source.createdAt).toLocaleString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
      downloadLabel: source.speedTest.download.mbps != null ? `${source.speedTest.download.mbps.toFixed(0)} Mbps` : '--',
      latencyLabel: source.speedTest.latency.ms != null ? `${source.speedTest.latency.ms} ms` : '--',
      qualityLabel: label,
      qualityLevel: level,
      uploadLabel: source.speedTest.upload.mbps != null ? `${source.speedTest.upload.mbps.toFixed(0)} Mbps` : '--',
    };
  })();

  return (
    <ThemeProvider mode={themeMode}>
      {installBanner}
      <HomeScreen
        historyCount={historyState.entries.length}
        latest={latest}
        onOpenAbout={goAbout}
        onOpenHistory={openHistory}
        onOpenSettings={openSettings}
        onStartTest={startTest}
      />
    </ThemeProvider>
  );
}
