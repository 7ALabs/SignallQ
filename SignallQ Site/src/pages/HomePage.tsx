import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdBannerWide } from '../components/AdBannerWide'
import { AdRail } from '../components/AdRail'
import { DetailTopBar, FlowTopBar } from '../components/AppTopBar'
import { SiteFooter } from '../components/SiteFooter'
import { SiteNav } from '../components/SiteNav'
import { IdleStart, type ModoTeste } from '../components/speedtest/IdleStart'
import { ProblemPanel } from '../components/speedtest/ProblemPanel'
import { ResultPanel } from '../components/speedtest/ResultPanel'
import { RunningPanel } from '../components/speedtest/RunningPanel'
import type { StepInfo } from '../components/speedtest/StepRow'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { type FasePainel, type ProblemPhase, useSpeedTest } from '../hooks/useSpeedTest'
import { classifyDownload } from '../lib/classification'
import { fractionForLatency, fractionForThroughput } from '../lib/gaugeMath'
import { PAGE_META } from '../lib/pageMetaCatalog'

const RUNNING_PHASES: FasePainel[] = ['preparando', 'latencia', 'download', 'upload', 'processando']
const PROBLEM_PHASES: ProblemPhase[] = ['sem-conexao', 'conexao-interrompida', 'endpoint-indisponivel', 'erro-inesperado', 'cancelado', 'bloqueado-outra-aba']
const STEP_ORDER: Array<'latencia' | 'download' | 'upload'> = ['latencia', 'download', 'upload']
const STEP_LABELS: Record<'latencia' | 'download' | 'upload', string> = { latencia: 'Latência', download: 'Download', upload: 'Upload' }
// Ícone por fase — mesmo vocabulário da grade de 3 métricas do Resultado
// (`ResultPanel.tsx`), usado no readout embutido do velocímetro durante o teste.
const PHASE_ICONS: Record<'latencia' | 'download' | 'upload', string> = {
  latencia: 'network_ping',
  download: 'arrow_downward',
  upload: 'arrow_upward',
}
const PHASE_READOUT_LABELS: Record<'latencia' | 'download' | 'upload', string> = {
  latencia: 'Latência',
  download: 'Download',
  upload: 'Upload',
}

function phaseColorVar(phase: FasePainel): string {
  if (phase === 'latencia') return 'var(--phase-latencia)'
  if (phase === 'download') return 'var(--phase-download)'
  if (phase === 'upload') return 'var(--phase-upload)'
  return 'var(--accent)'
}

// Fluxo do PWA (Home "/") — reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHome.dc.html`): uma única rota
// com máquina de estados por fase, chrome mínimo no mobile (FlowTopBar/DetailTopBar) e
// SiteNav completo no desktop, ladeado por 2 colunas de anúncio (AdRail a/b) — mesmo
// padrão de composição de todas as telas do site (SiteNav → AdRail(a) + conteúdo +
// AdRail(b) → AdBannerWide → SiteFooter, ver README do pacote de design-specs, achado 1).
// No estado de Resultado, o AdBannerWide de rodapé da página fica oculto — o card de
// resultado já embute o seu próprio anúncio (`ResultPanel.tsx`).
export default function HomePage() {
  useDocumentMeta(PAGE_META['/'])

  const navigate = useNavigate()
  const [modo, setModo] = useState<ModoTeste>('rapido')
  const { phase, liveValue, phaseResults, result, connectionKind, round, cancelTest, retry, forceStart, goToIdle } = useSpeedTest(modo)

  const isIdle = phase === 'idle'
  const isRunning = RUNNING_PHASES.includes(phase)
  const isResult = phase === 'concluido' || phase === 'parcial' || phase === 'inconclusivo' || phase === 'contaminado'
  const isProblem = PROBLEM_PHASES.includes(phase as ProblemPhase)
  const stepIdx = RUNNING_PHASES.indexOf(phase)
  const phaseColor = phaseColorVar(phase)
  const runningKey = phase === 'latencia' || phase === 'download' || phase === 'upload' ? phase : null

  const downloadVerdict = result ? classifyDownload(result.download.mbps) : null

  let fraction = 0
  let gaugeCenterValue = ''
  let gaugeCenterUnit = ''

  if (phase === 'latencia') {
    fraction = fractionForLatency(liveValue)
    gaugeCenterValue = liveValue ? Math.round(liveValue).toString() : '—'
    gaugeCenterUnit = 'ms'
  } else if (phase === 'download' || phase === 'upload') {
    fraction = fractionForThroughput(liveValue)
    gaugeCenterValue = liveValue ? liveValue.toFixed(1) : '0.0'
    gaugeCenterUnit = 'Mbps'
  } else if (phase === 'processando') {
    fraction = 1
  }

  const steps: StepInfo[] = STEP_ORDER.map((key) => {
    const idx = RUNNING_PHASES.indexOf(key)
    const done = idx < stepIdx || phase === 'processando'
    const active = key === phase
    const val = phaseResults[key]
    const unit = key === 'latencia' ? 'ms' : 'Mbps'
    let value = 'Aguardando'
    if (active) value = liveValue ? `${key === 'latencia' ? Math.round(liveValue) : liveValue.toFixed(1)} ${unit}` : '…'
    else if (done && val != null) value = `${key === 'latencia' ? Math.round(val) : val.toFixed(1)} ${unit}`
    return {
      label: STEP_LABELS[key],
      value,
      color: active ? phaseColor : done ? 'var(--text-primary)' : 'var(--text-tertiary)',
    }
  })

  const handleIniciar = () => {
    forceStart()
  }

  const irParaHistorico = () => navigate('/historico')

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <div className="hidden lg:block">
        <SiteNav active="home" />
      </div>
      <div className="lg:hidden">
        {isResult ? (
          <DetailTopBar title="Resultado" onBack={goToIdle} rightIcon="history" rightLabel="Ver histórico" onRightClick={irParaHistorico} />
        ) : (
          <FlowTopBar onHistoryClick={irParaHistorico} />
        )}
      </div>

      <div className="mx-auto flex w-full flex-1 items-start justify-center gap-6 px-4 pb-4 pt-2 box-border lg:gap-6 lg:px-6 lg:pt-5">
        <AdRail variant="a" />

        <div className={`flex w-full flex-1 flex-col items-center gap-5 ${isResult ? 'max-w-[620px]' : 'max-w-[860px]'}`}>
          {isIdle && <IdleStart modo={modo} onModoChange={setModo} onIniciar={handleIniciar} />}

          {isRunning && runningKey && (
            <RunningPanel
              fraction={fraction}
              color={phaseColor}
              phaseIcon={PHASE_ICONS[runningKey]}
              phaseLabel={PHASE_READOUT_LABELS[runningKey]}
              centerValue={gaugeCenterValue}
              centerUnit={gaugeCenterUnit}
              round={modo === 'triplo' ? round : null}
              steps={steps}
              onCancel={cancelTest}
            />
          )}

          {isProblem && <ProblemPanel phase={phase as ProblemPhase} onAction={phase === 'bloqueado-outra-aba' ? forceStart : retry} />}

          {isResult && result && downloadVerdict && (
            <ResultPanel
              result={result}
              downloadVerdict={downloadVerdict}
              connectionKind={connectionKind}
              onRetry={retry}
              onVerHistorico={irParaHistorico}
            />
          )}

          {/* Achado 1 do README de design-specs: `showWideAd` é `!isResult` — o
              Resultado já tem seu próprio anúncio embutido no card (ResultPanel). */}
          {!isResult && (
            <div className="mt-auto w-full pt-6">
              <AdBannerWide variant="a" />
            </div>
          )}

          {/* Seção secundária mobile-only (`showSecondary`, mobile && (isIdle || isResult))
              — no desktop, a coluna de anúncio (AdRail) e o AdBannerWide já ocupam esse
              espaço. 1:1 estrito com o protótipo (decisão do Luiz, 2026-07-25): só os 2
              cards de `ScreenHome.dc.html`; os 2 extras que existiam aqui (Diagnóstico no
              app / Sobre o SignallQ) foram removidos — reduz um CTA de download do app
              fora do Resultado, motivo já avaliado e descartado pelo Luiz. */}
          {(isIdle || isResult) && (
            <section className="w-full lg:hidden" aria-label="Entenda seu teste">
              <div className="grid gap-3">
                <article className="rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
                  <h2 className="title-large m-0">Entenda o resultado</h2>
                  <p className="body-medium mb-0 mt-2">
                    Download e upload mostram capacidade de transmissão. Latência e estabilidade ajudam a explicar como a conexão responde no uso real.
                  </p>
                </article>
                <article className="rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
                  <h2 className="title-large m-0">Por que pode variar</h2>
                  <p className="body-medium mb-0 mt-2">
                    Wi-Fi, distância do roteador, outros aparelhos, congestionamento e o próprio navegador podem alterar a medição.
                  </p>
                </article>
              </div>
            </section>
          )}
        </div>

        <AdRail variant="b" />
      </div>

      <SiteFooter />
    </div>
  )
}
