import { useState } from 'react'
import { KeyValueList } from '../KeyValueList'
import { NoticeBar } from '../NoticeBar'
import { PlayStoreBadge } from '../PlayStoreBadge'
import { MetricCard } from './MetricCard'
import { ResultAdCard } from './ResultAdCard'
import { classifyLatency, classifyUpload, interpretUseCases, type Classificacao } from '../../lib/classification'
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

const USE_CASE_ICONS = {
  navegacao: 'travel_explore',
  streaming: 'movie',
  videochamada: 'videocam',
  jogosOnline: 'sports_esports',
} as const
const USE_CASE_LABELS: Record<keyof typeof USE_CASE_ICONS, string> = {
  navegacao: 'Navegação',
  streaming: 'Streaming',
  videochamada: 'Videochamadas',
  jogosOnline: 'Jogos online',
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

// Resultado técnico do PWA — reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHome.dc.html`, variant "result"):
// pill de status com data, grade de 3 métricas, interpretação com casos de uso,
// contexto/detalhes técnicos lado a lado e o anúncio local embutido no card
// (achado 1 do README do pacote — 4º espaço de anúncio, distinto do
// `AdBannerWide` de rodapé da página, que fica oculto neste estado).
//
// Divergências deliberadas do protótipo (documentadas, não são omissão):
// - O protótipo mocka os ícones das 3 métricas sempre em verde/"Boa"; aqui a cor
//   e o ícone seguem o veredito real de cada métrica (download/upload/latência
//   podem divergir entre si).
// - Os cards "Contexto da execução"/"Detalhes técnicos" mantêm os campos extras
//   já existentes (conexão informada, DNS, latência sob carga, amostras) além
//   dos 2-3 campos do protótipo — são dados reais já testados, cortar seria
//   perda funcional sem ganho de design; a estrutura de 2 cards lado a lado
//   bate 1:1.
// - Compartilhar/Copiar resumo e o CTA de download do app não estão no
//   protótipo desta tela, mas são funcionalidade de produto real preexistente
//   (não a decisão de design cortar) — mantidos após o anúncio embutido.
export function ResultPanel({ result, downloadVerdict, connectionKind, onRetry, onVerHistorico }: ResultPanelProps) {
  const [copied, setCopied] = useState(false)
  const latency = classifyLatency(result.latency.ms)
  const uploadVerdict = classifyUpload(result.upload.mbps)
  const veredito = VEREDITO[downloadVerdict.nivel] ?? VEREDITO.indisponivel
  const mostrarChipConexao = connectionKind != null && connectionKind !== 'nenhuma' && connectionKind !== 'desconhecida'
  const completo = result.status === 'complete'
  const statusCopy = STATUS_COPY[result.status]
  const useCases = interpretUseCases({
    download: result.download.mbps,
    upload: result.upload.mbps,
    latency: result.latency.ms,
    jitter: result.jitter?.ms ?? null,
  })

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
    <div className="sq-fade-up flex w-full max-w-[620px] flex-col gap-4">
      <div className="flex items-center justify-center gap-2">
        <span className="material-symbols-outlined" style={{ fontSize: 18, color: completo ? 'var(--success)' : 'var(--warning)' }}>
          {completo ? 'check_circle' : 'warning'}
        </span>
        <span className="label-medium" style={{ color: completo ? 'var(--success)' : 'var(--warning)' }}>
          {statusCopy.title}
        </span>
        <span className="body-small" style={{ color: 'var(--text-tertiary)' }}>
          · {formatarDataHora(result.timestamp)}
        </span>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <MetricCard
          icon="arrow_downward"
          iconColor={NIVEL_COR[downloadVerdict.nivel]}
          label="Download"
          value={result.download.mbps.toFixed(1)}
          detail={`Mbps · ${downloadVerdict.label}`}
        />
        <MetricCard
          icon="arrow_upward"
          iconColor={NIVEL_COR[uploadVerdict.nivel]}
          label="Upload"
          value={result.upload.mbps.toFixed(1)}
          detail={`Mbps · ${uploadVerdict.label}`}
        />
        <MetricCard
          icon="network_ping"
          iconColor={NIVEL_COR[latency.nivel]}
          label="Latência"
          value={Math.round(result.latency.ms).toString()}
          detail={`ms · ${latency.label}`}
        />
      </div>

      {!completo && (
        <NoticeBar
          severity="warning"
          message={
            <>
              <b style={{ color: 'var(--text-primary)' }}>{statusCopy.title}.</b> {statusCopy.body}
            </>
          }
        />
      )}

      <section className="rounded-2xl p-4.5" style={{ background: 'color-mix(in srgb, var(--accent) 7%, transparent)' }} aria-labelledby="interpretacao-signallq">
        <div id="interpretacao-signallq" className="label-overline" style={{ color: 'var(--accent)' }}>
          Interpretação SignallQ
        </div>
        <div className="headline-small mt-1.5">{veredito.titulo}</div>
        <div className="body-medium mt-1">{veredito.subtitulo}</div>

        <div className="mt-3.5 grid grid-cols-2 gap-2.5 sm:grid-cols-4">
          {(Object.keys(USE_CASE_ICONS) as Array<keyof typeof USE_CASE_ICONS>).map((key) => {
            const veredictoCaso = useCases[key]
            return (
              <div
                key={key}
                className="flex min-h-[86px] flex-col items-center justify-center gap-1 rounded-xl px-2 py-3 text-center"
                style={{ background: 'var(--bg-primary)' }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 20, color: NIVEL_COR[veredictoCaso.nivel] }}>
                  {USE_CASE_ICONS[key]}
                </span>
                <span className="body-small" style={{ color: 'var(--text-secondary)' }}>
                  {USE_CASE_LABELS[key]}
                </span>
                <span className="label-medium" style={{ color: NIVEL_COR[veredictoCaso.nivel] }}>
                  {veredictoCaso.label}
                </span>
              </div>
            )
          })}
        </div>

        <p className="mt-3 body-small" style={{ color: 'var(--text-tertiary)' }}>
          Esta é uma leitura das métricas desta medição, não uma certificação da velocidade contratada.
        </p>
      </section>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <section className="rounded-2xl p-4" style={{ background: 'var(--bg-secondary)' }} aria-labelledby="contexto-execucao">
          <div id="contexto-execucao" className="label-overline">
            Contexto da execução
          </div>
          <KeyValueList
            className="mt-2.5"
            items={[
              { key: 'infraestrutura', label: 'Infraestrutura', value: result.server },
              ...(formatarDuracao(result.durationMs)
                ? [{ key: 'duracao', label: 'Duração', value: formatarDuracao(result.durationMs) }]
                : []),
              { key: 'data-hora', label: 'Data e hora local', value: formatarDataHora(result.timestamp) },
              ...(mostrarChipConexao
                ? [
                    {
                      key: 'conexao',
                      label: 'Conexão informada',
                      value: (
                        <span className="flex items-center justify-end gap-1">
                          <span className="material-symbols-outlined" style={{ fontSize: 14 }}>
                            {iconeConexao(connectionKind)}
                          </span>
                          Teste realizado via {labelConexao(connectionKind)}
                        </span>
                      ),
                    },
                  ]
                : []),
            ]}
          />
          <p className="mt-2.5 body-small" style={{ color: 'var(--text-tertiary)' }}>
            O navegador não confirma o seu provedor nem a sua localização. Por isso esses dados não são estimados aqui.
          </p>
        </section>

        <section className="rounded-2xl p-4" style={{ background: 'var(--bg-secondary)' }} aria-labelledby="detalhes-tecnicos">
          <div id="detalhes-tecnicos" className="label-overline">
            Detalhes técnicos
          </div>
          <KeyValueList
            className="mt-2.5"
            items={[
              { key: 'jitter', label: 'Jitter', value: result.jitter ? `${result.jitter.ms.toFixed(1)} ms` : 'Indisponível' },
              {
                key: 'bufferbloat',
                label: 'Bufferbloat',
                value: `${result.bufferbloat.ms.toFixed(1)} ms · ${BUFFERBLOAT_LABEL[result.bufferbloat.severity]}`,
              },
              { key: 'estabilidade', label: 'Estabilidade', value: `${result.stabilityScore.toFixed(0)}%` },
              { key: 'perda-http', label: 'Perda HTTP', value: `${result.packetLoss.percent.toFixed(1)}%` },
              {
                key: 'latencia-download',
                label: 'Latência sob download',
                value: result.loadedLatency ? `${result.loadedLatency.downloadMs.toFixed(0)} ms` : 'Indisponível',
              },
              {
                key: 'latencia-upload',
                label: 'Latência sob upload',
                value: result.loadedLatency ? `${result.loadedLatency.uploadMs.toFixed(0)} ms` : 'Indisponível',
              },
              { key: 'dns', label: 'DNS (DoH)', value: result.dns.latencyMs == null ? 'Indisponível' : `${result.dns.latencyMs} ms` },
              {
                key: 'amostras',
                label: 'Amostras de latência',
                value: `${result.latency.validSamples}/${result.latency.samples}`,
              },
            ]}
          />
          <p className="mt-2.5 body-small" style={{ color: 'var(--text-tertiary)' }}>
            Latência e perda são medidas por HTTPS; navegadores não permitem ICMP nem detectam todos os handovers de rede.
          </p>
        </section>
      </div>

      <a href="/como-medimos" className="self-center label-medium no-underline" style={{ color: 'var(--accent)' }}>
        Entenda como o teste mede sua conexão
      </a>

      <div className="flex gap-3">
        <button
          onClick={onRetry}
          className="flex h-[46px] flex-1 items-center justify-center gap-2 rounded-[var(--radius-button)]"
          style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
            refresh
          </span>
          <span className="label-large" style={{ color: 'var(--on-accent)' }}>
            Testar novamente
          </span>
        </button>
        <button
          onClick={onVerHistorico}
          className="flex h-[46px] flex-1 items-center justify-center gap-2 rounded-[var(--radius-button)] border"
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

      {/* 4º espaço de anúncio (achado 1 do README de design-specs): embutido dentro do
          próprio card de resultado, distinto do AdBannerWide de rodapé da página (oculto
          neste estado — ver `showWideAd` em HomePage.tsx). Peça dedicada (`ResultAdCard`),
          não o `AdBannerWide` genérico — achado da auditoria 1:1 de 2026-07-25 (Marina). */}
      <ResultAdCard />

      <div className="flex items-center gap-3 rounded-2xl p-4" style={{ background: 'color-mix(in srgb, var(--accent) 8%, transparent)' }}>
        <div className="flex flex-1 flex-col gap-0.5">
          <div className="label-large">Quer saber o motivo da sua velocidade?</div>
          <div className="body-small">Diagnóstico completo no app SignallQ.</div>
        </div>
        <PlayStoreBadge height={40} source="resultado-cta" />
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
        <div className="self-center label-medium" style={{ color: 'var(--success)' }}>
          Copiado!
        </div>
      )}
    </div>
  )
}
