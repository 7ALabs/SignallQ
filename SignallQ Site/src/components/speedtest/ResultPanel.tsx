import { useState } from 'react'
import { PlayStoreBadge } from '../PlayStoreBadge'
import { classifyLatency, classifyUpload, type Classificacao } from '../../lib/classification'
import { iconeConexao, labelConexao, type TipoRede } from '../../lib/connection'
import { FEATURE_SPEEDTEST_COMPARTILHOU, trackFeatureUsed } from '../../lib/telemetry'
import type { SpeedTestResult } from '../../lib/speedEngine'

const NIVEL_COR: Record<string, string> = {
  success: 'var(--success)',
  warning: 'var(--warning)',
  error: 'var(--error)',
  indisponivel: 'var(--text-tertiary)',
}

// Frase-veredito da tela de Resultado — protótipo "SignallQ WebApp.dc.html"
// do Luiz pede "frase direta" (ex.: "Sua conexão está boa"), não um label
// solto tipo Excelente/Boa/Ruim. Copy é decisão de produto minha (Camilo),
// não vinha especificada linha a linha no handoff — sinalizada no resumo da
// entrega, Claudete/Lia podem querer revisar o texto.
const VEREDITO: Record<string, { titulo: string; subtitulo: string }> = {
  success: { titulo: 'Sua conexão está boa', subtitulo: 'Dá para navegar, assistir e jogar sem grandes travamentos.' },
  warning: { titulo: 'Sua conexão está aceitável', subtitulo: 'Funciona para a maioria dos usos, mas pode engasgar em tarefas mais pesadas.' },
  error: { titulo: 'Sua conexão está fraca', subtitulo: 'Streaming, chamadas e jogos online podem travar ou ficar lentos.' },
  indisponivel: { titulo: 'Não deu para avaliar sua conexão', subtitulo: 'Tente novamente para ver um veredito completo.' },
}

function formattedSummary(result: SpeedTestResult): string {
  const when = new Date(result.timestamp).toLocaleString('pt-BR')
  return `Meu teste de velocidade SignallQ (${when}): Download ${result.download.mbps.toFixed(1)} Mbps · Upload ${result.upload.mbps.toFixed(1)} Mbps · Latência ${Math.round(result.latency.ms)} ms. Teste a sua em ${location.origin}${location.pathname}`
}

function formatarDataHora(timestamp: number): string {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(timestamp))
}

function formatarDuracao(durationMs: number | undefined): string | null {
  if (durationMs == null || durationMs <= 0) return null
  return `${Math.max(1, Math.round(durationMs / 1000))} s`
}

const STATUS_COPY: Record<SpeedTestResult['status'], { title: string; body: string }> = {
  complete: { title: 'Medição completa', body: 'Todas as fases tiveram dados suficientes para uma avaliação confiável.' },
  partial: { title: 'Resultado parcial', body: 'Alguma fase não terminou. Use apenas as métricas disponíveis.' },
  inconclusive: { title: 'Resultado inconclusivo', body: 'Não houve amostras de latência suficientes. Repita o teste.' },
  contaminated: { title: 'Rede alterada durante o teste', body: 'A conexão mudou durante a medição; repita para obter um resultado confiável.' },
  cancelled: { title: 'Teste cancelado', body: 'A medição foi interrompida antes de terminar.' },
}

const BUFFERBLOAT_LABEL: Record<SpeedTestResult['bufferbloat']['severity'], string> = {
  none: 'Nenhum',
  mild: 'Leve',
  moderate: 'Moderado',
  severe: 'Severo',
}

interface ResultPanelProps {
  result: SpeedTestResult
  downloadVerdict: Classificacao
  connectionKind: TipoRede | null
  onRetry: () => void
  onVerHistorico: () => void
}

// Resultado técnico do PWA: mantém o veredito acessível no topo e agrupa os
// dados de auditoria em disclosure progressivo, sem virar dashboard denso.
export function ResultPanel({ result, downloadVerdict, connectionKind, onRetry, onVerHistorico }: ResultPanelProps) {
  const [copied, setCopied] = useState(false)
  const latency = classifyLatency(result.latency.ms)
  const uploadVerdict = classifyUpload(result.upload.mbps)
  const veredito = VEREDITO[downloadVerdict.nivel] ?? VEREDITO.indisponivel
  const mostrarChipConexao = connectionKind != null && connectionKind !== 'nenhuma' && connectionKind !== 'desconhecida'

  const copySummary = async (fromShareFallback: boolean) => {
    const text = formattedSummary(result)
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
      trackFeatureUsed(FEATURE_SPEEDTEST_COMPARTILHOU)
    } catch {
      window.prompt('Copie o resumo abaixo:', text)
      if (!fromShareFallback) trackFeatureUsed(FEATURE_SPEEDTEST_COMPARTILHOU)
    }
  }

  const share = async () => {
    const text = formattedSummary(result)
    if (navigator.share) {
      try {
        await navigator.share({ title: 'Meu teste de velocidade SignallQ', text, url: location.href })
        trackFeatureUsed(FEATURE_SPEEDTEST_COMPARTILHOU)
        return
      } catch {
        // usuário cancelou o share nativo — cai no fallback de cópia
      }
    }
    await copySummary(true)
  }

  return (
    <div className="sq-fade-up flex w-full max-w-[460px] flex-col items-center gap-5 pt-2">
      {result.status !== 'complete' && (
        <div className="flex items-center gap-2 text-center">
          <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--warning)' }}>
            warning
          </span>
          <div className="body-small">
            <b style={{ color: 'var(--text-primary)' }}>{STATUS_COPY[result.status].title}.</b> {STATUS_COPY[result.status].body}
          </div>
        </div>
      )}

      <div className="flex w-full gap-3">
        <div className="flex flex-1 flex-col items-center gap-1 rounded-2xl py-4" style={{ background: 'var(--bg-secondary)' }}>
          <div className="overline">Download</div>
          <div className="title-large" style={{ color: NIVEL_COR[downloadVerdict.nivel] }}>
            {result.download.mbps.toFixed(1)} <span className="label-medium">Mbps</span>
          </div>
        </div>
        <div className="flex flex-1 flex-col items-center gap-1 rounded-2xl py-4" style={{ background: 'var(--bg-secondary)' }}>
          <div className="overline">Upload</div>
          <div className="title-large" style={{ color: NIVEL_COR[uploadVerdict.nivel] }}>
            {result.upload.mbps.toFixed(1)} <span className="label-medium">Mbps</span>
          </div>
        </div>
      </div>

      <div className="flex w-full items-center justify-between px-1">
        <span className="body-medium">Latência</span>
        <span className="label-large" style={{ color: NIVEL_COR[latency.nivel] }}>
          {Math.round(result.latency.ms)} ms · {latency.label}
        </span>
      </div>

      <section className="w-full rounded-2xl p-4" style={{ background: 'var(--bg-secondary)' }} aria-labelledby="contexto-execucao">
        <div id="contexto-execucao" className="overline">Contexto da execução</div>
        <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 body-small">
          <dt>Data e hora local</dt><dd className="text-right">{formatarDataHora(result.timestamp)}</dd>
          <dt>Infraestrutura</dt><dd className="text-right">{result.server}</dd>
          {mostrarChipConexao && <><dt>Conexão informada</dt><dd className="flex items-center justify-end gap-1 text-right"><span className="material-symbols-outlined" style={{ fontSize: 14 }}>{iconeConexao(connectionKind)}</span>Teste realizado via {labelConexao(connectionKind)}</dd></>}
          {formatarDuracao(result.durationMs) && <><dt>Duração</dt><dd className="text-right">{formatarDuracao(result.durationMs)}</dd></>}
        </dl>
        <p className="mt-3 body-small" style={{ color: 'var(--text-tertiary)' }}>
          O navegador não confirma o seu provedor nem a sua localização. Por isso esses dados não são estimados aqui.
        </p>
      </section>

      <section className="w-full rounded-2xl p-4" style={{ background: 'color-mix(in srgb, var(--accent) 8%, transparent)' }} aria-labelledby="interpretacao-signallq">
        <div id="interpretacao-signallq" className="overline" style={{ color: 'var(--accent)' }}>Interpretação SignallQ</div>
        <div className="headline-small mt-2">{veredito.titulo}</div>
        <div className="body-medium mt-1">{veredito.subtitulo}</div>
        <p className="mt-3 body-small" style={{ color: 'var(--text-tertiary)' }}>
          Esta é uma leitura das métricas desta medição, não uma certificação da velocidade contratada.
        </p>
      </section>

      <details className="w-full rounded-2xl p-4" style={{ background: 'var(--bg-secondary)' }}>
        <summary className="label-large cursor-pointer" style={{ color: 'var(--text-primary)' }}>Detalhes técnicos</summary>
        <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 body-small">
          <dt>Jitter</dt><dd className="text-right">{result.jitter ? `${result.jitter.ms.toFixed(1)} ms` : 'Indisponível'}</dd>
          <dt>Perda HTTP</dt><dd className="text-right">{result.packetLoss.percent.toFixed(1)}%</dd>
          <dt>Bufferbloat</dt><dd className="text-right">{result.bufferbloat.ms.toFixed(1)} ms · {BUFFERBLOAT_LABEL[result.bufferbloat.severity]}</dd>
          <dt>Estabilidade</dt><dd className="text-right">{result.stabilityScore.toFixed(0)}%</dd>
          <dt>Latência sob download</dt><dd className="text-right">{result.loadedLatency ? `${result.loadedLatency.downloadMs.toFixed(0)} ms` : 'Indisponível'}</dd>
          <dt>Latência sob upload</dt><dd className="text-right">{result.loadedLatency ? `${result.loadedLatency.uploadMs.toFixed(0)} ms` : 'Indisponível'}</dd>
          <dt>DNS (DoH)</dt><dd className="text-right">{result.dns.latencyMs == null ? 'Indisponível' : `${result.dns.latencyMs} ms`}</dd>
          <dt>Amostras de latência</dt><dd className="text-right">{result.latency.validSamples}/{result.latency.samples}</dd>
        </dl>
        <p className="mt-3 body-small" style={{ color: 'var(--text-tertiary)' }}>
          Latência e perda são medidas por HTTPS; navegadores não permitem ICMP nem detectam todos os handovers de rede.
        </p>
      </details>

      <a href="/como-medimos" className="label-medium no-underline" style={{ color: 'var(--accent)' }}>
        Entenda como o teste mede sua conexão
      </a>

      <div className="flex w-full items-center gap-3 rounded-2xl p-4" style={{ background: 'color-mix(in srgb, var(--accent) 8%, transparent)' }}>
        <div className="flex flex-1 flex-col gap-0.5">
          <div className="label-large">Quer saber o motivo da sua velocidade?</div>
          <div className="body-small">Diagnóstico completo no app SignallQ.</div>
        </div>
        <PlayStoreBadge height={40} source="resultado-cta" />
      </div>

      <div className="flex w-full flex-col gap-2.5">
        <button
          onClick={onRetry}
          className="flex h-[46px] w-full items-center justify-center gap-2 rounded-[var(--radius-button)] text-white"
          style={{ background: 'var(--accent)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
            refresh
          </span>
          <span className="label-large" style={{ color: '#fff' }}>
            Testar novamente
          </span>
        </button>
        <button
          onClick={onVerHistorico}
          className="flex h-[46px] w-full items-center justify-center gap-2 rounded-[var(--radius-button)] border"
          style={{ borderColor: 'var(--border)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--accent)' }}>
            history
          </span>
          <span className="label-large" style={{ color: 'var(--accent)' }}>
            Ver histórico
          </span>
        </button>
      </div>

      <div className="flex flex-wrap justify-center gap-2.5">
        <button onClick={share} className="flex h-9 items-center gap-1.5 border-none bg-transparent px-2">
          <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--accent)' }}>
            share
          </span>
          <span className="label-medium" style={{ color: 'var(--accent)' }}>
            Compartilhar
          </span>
        </button>
        <button onClick={() => copySummary(false)} className="flex h-9 items-center gap-1.5 border-none bg-transparent px-2">
          <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--accent)' }}>
            content_copy
          </span>
          <span className="label-medium" style={{ color: 'var(--accent)' }}>
            Copiar resumo
          </span>
        </button>
      </div>
      {copied && (
        <div className="label-medium" style={{ color: 'var(--success)' }}>
          Copiado!
        </div>
      )}
    </div>
  )
}
