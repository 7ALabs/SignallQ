import type { AdminDiagnosticPayload, DiagnosisResult, SpeedTestResult } from '@shared/contracts';

/**
 * Ambiente do PWA (GH#441/GH#442).
 *
 * O navegador nao tem um equivalente ao "canal de distribuicao" do Android
 * (Play Store vs sideload) para decidir production/staging. A unica distincao
 * honesta hoje e local (dev server) vs qualquer outro host publicado.
 */
function resolvePwaEnvironment(): 'production' | 'staging' {
  if (typeof window === 'undefined') return 'production';
  const host = window.location.hostname;
  return host === 'localhost' || host === '127.0.0.1' ? 'staging' : 'production';
}

/**
 * Monta o payload de POST /ingest/diagnostic a partir de um teste concluido no PWA.
 *
 * Campos que o navegador nao consegue medir com seguranca (operadora, tipo
 * fisico de rede wifi/celular, modelo/versao de dispositivo) sao deixados de
 * fora em vez de inventados — a Network Information API so expoe velocidade
 * estimada (effectiveType), nao o meio fisico da conexao.
 */
export function buildAdminDiagnosticPayload(speedTest: SpeedTestResult, diagnosis: DiagnosisResult): AdminDiagnosticPayload {
  const payload: AdminDiagnosticPayload = {
    id: speedTest.id,
    created_at: Math.floor(new Date(speedTest.measuredAt).getTime() / 1000),
    network_type: 'unknown',
    status: 'completed',
    download_mbps: speedTest.download.mbps,
    upload_mbps: speedTest.upload.mbps,
    latency_ms: speedTest.latency.ms,
    jitter_ms: speedTest.jitter.ms,
    packet_loss: speedTest.availability.perceivedLossPercent,
    environment: resolvePwaEnvironment(),
    platform: 'web',
  };

  if (diagnosis.source === 'ai') {
    payload.ai_summary_report = diagnosis.summary;
  }

  return payload;
}
