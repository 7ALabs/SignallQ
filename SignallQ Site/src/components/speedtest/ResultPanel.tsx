import { useState } from 'react'
import { PlayStoreBadge } from '../PlayStoreBadge'
import { classifyLatency, type Classificacao } from '../../lib/classification'
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

interface ResultPanelProps {
  result: SpeedTestResult
  downloadVerdict: Classificacao
  connectionKind: TipoRede | null
  onRetry: () => void
  onVerHistorico: () => void
}

// Versão enxuta da tela de Resultado do PWA (Tela 2 do protótipo "SignallQ
// WebApp.dc.html", GH#1186) — sem recomendações, sem grid de casos de uso,
// sem jitter, sem toggle de "detalhes técnicos" (removidos por decisão
// explícita do Luiz, não é engano/regressão). O motor de recomendações
// (lib/recommendations.ts) e o card (RecommendationsCard.tsx) continuam no
// código, só desconectados desta tela — podem voltar a ser usados depois.
export function ResultPanel({ result, downloadVerdict, connectionKind, onRetry, onVerHistorico }: ResultPanelProps) {
  const [copied, setCopied] = useState(false)
  const latency = classifyLatency(result.latency.ms)
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
      {result.partial && (
        <div className="flex items-center gap-2 text-center">
          <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--warning)' }}>
            warning
          </span>
          <div className="body-small">
            <b style={{ color: 'var(--text-primary)' }}>Resultado parcial.</b> A conexão foi interrompida — os números refletem só a parte
            confiável do teste.
          </div>
        </div>
      )}

      <div className="flex flex-col items-center gap-2 text-center">
        <div className="headline-small">{veredito.titulo}</div>
        <div className="body-medium">{veredito.subtitulo}</div>
        {mostrarChipConexao && (
          <div className="mt-1 flex items-center gap-1.5 rounded-full border px-3 py-1" style={{ borderColor: 'var(--border)' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 14, color: 'var(--text-tertiary)' }}>
              {iconeConexao(connectionKind)}
            </span>
            <span className="label-small">Teste realizado via {labelConexao(connectionKind)}</span>
          </div>
        )}
      </div>

      <div className="flex w-full gap-3">
        <div className="flex flex-1 flex-col items-center gap-1 rounded-2xl py-4" style={{ background: 'var(--bg-secondary)' }}>
          <div className="overline">Download</div>
          <div className="title-large" style={{ color: 'var(--success)' }}>
            {result.download.mbps.toFixed(1)} <span className="label-medium">Mbps</span>
          </div>
        </div>
        <div className="flex flex-1 flex-col items-center gap-1 rounded-2xl py-4" style={{ background: 'var(--bg-secondary)' }}>
          <div className="overline">Upload</div>
          <div className="title-large" style={{ color: 'var(--warning)' }}>
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
