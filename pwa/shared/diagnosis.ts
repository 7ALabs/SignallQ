import type {
  DiagnosisInput,
  DiagnosisResult,
  QualityClassification,
  RecommendedAction,
  LegacySpeedtestResult,
  SpeedTestResult,
  StabilityClassification,
} from './contracts';

/**
 * Thresholds alinhados ao motor de diagnóstico Android (GH#438).
 *
 * Fonte de verdade: `InternetDiagnosticEngine.kt` (feature/diagnostico) e
 * `MetricClassifier.kt`, que por sua vez seguem a skill `/regras-diagnostico-rede`.
 * Não reintroduzir valores diferentes destes sem atualizar os dois lados.
 *
 * Diferenças mantidas por limitação real do navegador (documentadas como
 * `limitations` no resultado, nunca escondidas do usuário):
 * - latência/jitter vêm de HTTP timing, não de ping ICMP nativo;
 * - perda de pacotes é inferida por falha de requisição HTTP, não ICMP;
 * - bufferbloat (IN-NORMAL-09 no Android) não é medido — exigiria uma fase de
 *   "download simultâneo + ping" que o speedtest web atual não implementa.
 */
const DOWNLOAD_LOW_MBPS = 25; // Android IN-NORMAL-03 "Download Baixo"
const DOWNLOAD_FAST_MBPS = 100; // só PWA: refina o rótulo exibido acima do mínimo saudável
const UPLOAD_LOW_MBPS = 5; // Android IN-NORMAL-04 "Upload Baixo"
const LATENCY_ATTENTION_MS = 100; // Android IN-NORMAL-05 (referência Anatel RQUAL)
const JITTER_ATTENTION_MS = 20; // Android IN-NORMAL-06 "Jitter Elevado"
const PACKET_LOSS_ATTENTION_PERCENT = 1; // Android IN-NORMAL-07b "Perda de Pacotes Moderada"
const PACKET_LOSS_CRITICAL_PERCENT = 3; // Android IN-NORMAL-07 "Perda de Pacotes Alta"

interface ReadMetrics {
  downloadMbps: number | null;
  uploadMbps: number | null;
  latencyMs: number | null;
  jitterMs: number | null;
  packetLossPercent: number | null;
}

function readMetrics(speedTest: SpeedTestResult | LegacySpeedtestResult | null | undefined): ReadMetrics {
  if (!speedTest) {
    return { downloadMbps: null, uploadMbps: null, latencyMs: null, jitterMs: null, packetLossPercent: null };
  }

  if ('download' in speedTest) {
    return {
      downloadMbps: speedTest.download.mbps,
      uploadMbps: speedTest.upload.mbps,
      latencyMs: speedTest.latency.ms,
      jitterMs: speedTest.jitter.ms,
      packetLossPercent: speedTest.availability.perceivedLossPercent,
    };
  }

  return {
    downloadMbps: speedTest.downloadMbps,
    uploadMbps: speedTest.uploadMbps,
    latencyMs: speedTest.latencyMs,
    jitterMs: speedTest.jitterMs ?? null,
    packetLossPercent: null,
  };
}

export function classifySpeed(downloadMbps: number | null): 'fast' | 'ok' | 'slow' | 'unknown' {
  if (downloadMbps == null) return 'unknown';
  if (downloadMbps < DOWNLOAD_LOW_MBPS) return 'slow';
  if (downloadMbps < DOWNLOAD_FAST_MBPS) return 'ok';
  return 'fast';
}

export function classifyStability(latencyMs: number | null, jitterMs: number | null): StabilityClassification {
  if (latencyMs == null && jitterMs == null) return 'unknown';
  if (
    (latencyMs != null && latencyMs > LATENCY_ATTENTION_MS) ||
    (jitterMs != null && jitterMs > JITTER_ATTENTION_MS)
  ) {
    return 'unstable';
  }
  return 'stable';
}

export function buildSummary(
  speed: 'fast' | 'ok' | 'slow' | 'unknown',
  stability: StabilityClassification,
): string {
  if (speed === 'unknown' && stability === 'unknown') {
    return 'Não foi possível medir dados suficientes no navegador. Tente novamente com a conexão ativa.';
  }
  if (speed === 'slow') {
    return 'A velocidade medida está baixa para usos comuns. Confira se há outros aparelhos consumindo a rede.';
  }
  if (stability === 'unstable') {
    return 'A conexão parece instável pelo teste HTTP. Chamadas de vídeo e jogos podem oscilar.';
  }
  return 'A conexão parece adequada no teste web. Algumas métricas nativas não estão disponíveis no navegador.';
}

export function createLocalDiagnosis(input: DiagnosisInput = {}): DiagnosisResult {
  const metrics = readMetrics(input.speedTest);
  const speed = classifySpeed(metrics.downloadMbps);
  const stability = classifyStability(metrics.latencyMs, metrics.jitterMs);

  const internetIndisponivel = metrics.downloadMbps == null;
  const uploadZerado = metrics.uploadMbps === 0;
  const uploadBaixo = metrics.uploadMbps != null && metrics.uploadMbps > 0 && metrics.uploadMbps < UPLOAD_LOW_MBPS;
  const perdaCritica =
    metrics.packetLossPercent != null && metrics.packetLossPercent >= PACKET_LOSS_CRITICAL_PERCENT;
  const perdaModerada =
    !perdaCritica &&
    metrics.packetLossPercent != null &&
    metrics.packetLossPercent >= PACKET_LOSS_ATTENTION_PERCENT;

  const semDadosSuficientes = metrics.downloadMbps == null && metrics.latencyMs == null && metrics.jitterMs == null;

  const quality: QualityClassification = semDadosSuficientes
    ? 'unknown'
    : internetIndisponivel || uploadZerado || perdaCritica
      ? 'bad'
      : speed === 'slow' ||
          stability === 'unstable' ||
          uploadBaixo ||
          perdaModerada ||
          metrics.latencyMs == null ||
          metrics.jitterMs == null
        ? 'attention'
        : 'good';

  let summary = buildSummary(speed, stability);
  if (internetIndisponivel) {
    summary = 'Não foi possível medir o download no navegador. A internet pode estar sem acesso.';
  } else if (uploadZerado) {
    summary = 'O upload medido foi 0 Mbps. Isso costuma travar chamadas de vídeo, jogos online e envio de arquivos.';
  } else if (perdaCritica) {
    summary = 'Há perda de pacotes alta no teste web. Chamadas de vídeo e jogos serão gravemente afetados.';
  } else if (uploadBaixo) {
    summary = 'O upload está abaixo de 5 Mbps. Chamadas de vídeo e envio de arquivos podem ser afetados.';
  } else if (perdaModerada) {
    summary = 'Há alguma perda de pacotes no teste web. Jogos e chamadas podem ser afetados.';
  }

  const limitations: DiagnosisResult['limitations'] = [
    {
      code: 'http_latency_not_icmp_ping',
      message: 'A latência web usa requisições HTTP, não ping ICMP nativo.',
    },
    {
      code: 'browser_limited_measurement',
      message: 'O navegador não expõe sinal Wi-Fi real, scan de rede ou métricas de rádio.',
    },
    {
      code: 'bufferbloat_not_measured',
      message: 'O navegador não mede bufferbloat (latência sob carga) — essa checagem existe só no app Android.',
    },
  ];

  if (metrics.jitterMs == null) {
    limitations.push({
      code: 'jitter_not_measured',
      message: 'Jitter depende de amostras suficientes de latência.',
    });
  }

  if (metrics.packetLossPercent == null) {
    limitations.push({
      code: 'packet_loss_not_directly_measured',
      message: 'Perda de pacotes é inferida por falhas de requisição HTTP, não por ping ICMP.',
    });
  }

  const actions: RecommendedAction[] = [];
  if (uploadZerado) {
    actions.push({
      priority: 1,
      title: 'Reinicie roteador e modem',
      description: 'Upload zerado costuma indicar bloqueio no roteador ou instabilidade no link. Se persistir, contate o provedor.',
      category: 'router',
    });
  } else if (perdaCritica) {
    actions.push({
      priority: 1,
      title: 'Reinicie roteador e modem',
      description: 'Perda de pacotes alta prejudica chamadas de vídeo e jogos. Reiniciar os equipamentos costuma resolver instabilidades temporárias.',
      category: 'router',
    });
  } else if (uploadBaixo || perdaModerada) {
    actions.push({
      priority: 1,
      title: 'Verifique outros dispositivos na rede',
      description: 'Uploads ou downloads simultâneos em outros aparelhos podem estar consumindo a rede durante o teste.',
      category: 'device',
    });
  }

  actions.push({
    priority: 1,
    title: 'Repita o teste perto do roteador',
    description: 'Compare o resultado em outro ponto da casa para separar velocidade de estabilidade.',
    category: 'retry',
  });
  actions.push({
    priority: 1,
    title: 'Evite downloads durante a medição',
    description: 'Outros usos da rede podem alterar o resultado medido pelo navegador.',
    category: 'device',
  });

  const rankedActions = actions.slice(0, 3).map((action, index) => ({
    ...action,
    priority: (index + 1) as 1 | 2 | 3,
  }));

  return {
    id: `diag_${Date.now().toString(36)}`,
    generatedAt: new Date().toISOString(),
    source: 'local',
    summary,
    quality,
    speed,
    stability,
    actions: rankedActions,
    limitations,
    confidence: speed === 'unknown' ? 'low' : metrics.jitterMs == null ? 'medium' : 'high',
  };
}
