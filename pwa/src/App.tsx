import { Activity, BrainCircuit, Cloud, Gauge, RadioTower, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { MetricCard } from '@/components/MetricCard';
import { useConnectionSnapshot } from '@/hooks/useConnectionSnapshot';
import { requestAiDiagnosis, runSpeedtestProbe, sendAdminDiagnostic } from '@/services/api';
import { DiagnosticPayload, SpeedtestPhase, SpeedtestResult } from '@/types/network';

function formatMetric(value: number | null): string {
  return value == null ? 'N/A' : value.toLocaleString('pt-BR', { maximumFractionDigits: 1 });
}

function buildDiagnosticPayload(speedtest: SpeedtestResult | null, connectionType: string): DiagnosticPayload {
  return {
    schemaVersion: 'pwa_foundation_v1',
    source: 'pwa',
    connectionType,
    metricasAtuais: {
      downloadMbps: speedtest?.downloadMbps ?? null,
      uploadMbps: speedtest?.uploadMbps ?? null,
      latenciaMs: speedtest?.latencyMs ?? null,
    },
  };
}

export function App() {
  const connection = useConnectionSnapshot();
  const [phase, setPhase] = useState<SpeedtestPhase>(SpeedtestPhase.Idle);
  const [speedtest, setSpeedtest] = useState<SpeedtestResult | null>(null);
  const [aiStatus, setAiStatus] = useState('Aguardando diagnóstico');
  const [adminStatus, setAdminStatus] = useState('Proxy pronto');

  const connectionLabel = connection.effectiveType ?? (connection.status === 'online' ? 'online' : 'offline');

  const runProbe = async () => {
    setPhase(SpeedtestPhase.Latency);
    setAiStatus('Aguardando diagnóstico');
    const result = await runSpeedtestProbe();
    if (!result.ok || !result.data) {
      setPhase(SpeedtestPhase.Error);
      setAiStatus(result.error ?? 'Falha no speedtest');
      return;
    }

    setSpeedtest(result.data);
    setPhase(SpeedtestPhase.Complete);
  };

  const diagnose = async () => {
    const payload = buildDiagnosticPayload(speedtest, connectionLabel);
    setAiStatus('Chamando proxy de IA...');
    const result = await requestAiDiagnosis(payload);
    setAiStatus(result.ok ? 'Proxy de IA respondeu com sucesso' : `IA indisponível: ${result.error}`);
  };

  const ingest = async () => {
    const id = `pwa_${Date.now().toString(36)}`;
    setAdminStatus('Enviando via proxy server-side...');
    const result = await sendAdminDiagnostic({
      id,
      created_at: Math.floor(Date.now() / 1000),
      network_type: connectionLabel,
      status: speedtest ? 'completed' : 'unknown',
      score: null,
      download_mbps: speedtest?.downloadMbps ?? null,
      upload_mbps: speedtest?.uploadMbps ?? null,
      latency_ms: speedtest?.latencyMs ?? null,
      jitter_ms: null,
      packet_loss: null,
      issues: [],
      environment: 'preview',
      dist_channel: 'pwa',
      build_type: 'web',
      version_code: 1,
      device_id: 'pwa-anonymous',
    });
    setAdminStatus(result.ok ? 'Ingest Admin aceito pelo proxy' : `Ingest indisponível: ${result.error}`);
  };

  return (
    <main className="app-shell">
      <header className="top-bar">
        <img src="/icon-192.png" alt="" className="brand-mark" />
        <div>
          <p className="overline">SignallQ PWA</p>
          <h1>Fundação web pronta para o roadmap</h1>
        </div>
      </header>

      <section className="status-band" aria-label="Status da fundação">
        <div>
          <p className="overline">Arquitetura</p>
          <h2>Browser seguro, Workers no servidor</h2>
          <p>
            O frontend chama somente rotas locais <strong>/api</strong>. IA, ingest Admin e speedtest passam por
            Cloudflare Pages Functions para evitar CORS solto e segredo no bundle.
          </p>
        </div>
        <div className="architecture-list">
          <span><ShieldCheck size={18} /> INGEST_KEY server-side</span>
          <span><Cloud size={18} /> Pages Functions</span>
          <span><RadioTower size={18} /> Speedtest sem cache</span>
        </div>
      </section>

      <section className="metric-grid" aria-label="Métricas atuais">
        <MetricCard icon={<Activity size={24} />} label="Conexão" value={connectionLabel} tone="success" />
        <MetricCard icon={<Gauge size={24} />} label="Latência HTTP" value={formatMetric(speedtest?.latencyMs ?? null)} unit="ms" />
        <MetricCard icon={<RadioTower size={24} />} label="Download" value={formatMetric(speedtest?.downloadMbps ?? null)} unit="Mbps" tone="accent" />
        <MetricCard icon={<RadioTower size={24} />} label="Upload" value={formatMetric(speedtest?.uploadMbps ?? null)} unit="Mbps" tone="warning" />
      </section>

      <section className="control-panel" aria-label="Validações iniciais">
        <div>
          <p className="overline">Validação M1</p>
          <h2>Três bloqueios tratados</h2>
          <p>
            Esta tela é o primeiro cockpit técnico do PWA: mede uma amostra web, valida o proxy da IA e envia ingest
            de diagnóstico pelo servidor quando as variáveis estiverem configuradas.
          </p>
        </div>
        <div className="actions">
          <button type="button" onClick={runProbe}>
            <Gauge size={18} /> Medir amostra
          </button>
          <button type="button" onClick={diagnose}>
            <BrainCircuit size={18} /> Validar IA
          </button>
          <button type="button" onClick={ingest}>
            <ShieldCheck size={18} /> Validar Admin
          </button>
        </div>
        <div className="result-log" aria-live="polite">
          <p><strong>Speedtest:</strong> {phase}</p>
          <p><strong>IA:</strong> {aiStatus}</p>
          <p><strong>Admin:</strong> {adminStatus}</p>
          {!connection.browserSupportsNetworkInfo ? (
            <p>Tipo de conexão detalhado indisponível neste navegador. O PWA não vai inventar sinal, RSSI ou rede Wi-Fi.</p>
          ) : null}
        </div>
      </section>
    </main>
  );
}
