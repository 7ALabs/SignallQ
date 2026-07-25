import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdBanner } from '../components/AdBanner'
import { DetailTopBar, FlowTopBar } from '../components/AppTopBar'
import { IdleStart, type ModoTeste } from '../components/speedtest/IdleStart'
import { ProblemPanel } from '../components/speedtest/ProblemPanel'
import { ResultPanel } from '../components/speedtest/ResultPanel'
import { SpeedGauge } from '../components/speedtest/SpeedGauge'
import { StepRow, type StepInfo } from '../components/speedtest/StepRow'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { type FasePainel, type ProblemPhase, useSpeedTest } from '../hooks/useSpeedTest'
import { classifyDownload } from '../lib/classification'
import { fractionForLatency, fractionForThroughput } from '../lib/gaugeMath'
import { PAGE_META } from '../lib/pageMetaCatalog'

const RUNNING_PHASES: FasePainel[] = ['preparando', 'latencia', 'download', 'upload', 'processando']
const PROBLEM_PHASES: ProblemPhase[] = ['sem-conexao', 'conexao-interrompida', 'endpoint-indisponivel', 'erro-inesperado', 'cancelado', 'bloqueado-outra-aba']
const STEP_ORDER: Array<'latencia' | 'download' | 'upload'> = ['latencia', 'download', 'upload']
const STEP_LABELS: Record<'latencia' | 'download' | 'upload', string> = { latencia: 'Latência', download: 'Download', upload: 'Upload' }
const PHASE_LABELS: Record<string, string> = {
  preparando: 'Preparando conexão',
  latencia: 'Medindo latência',
  download: 'Medindo download',
  upload: 'Medindo upload',
  processando: 'Processando resultado',
}

function phaseColorVar(phase: FasePainel): string {
  if (phase === 'latencia') return 'var(--phase-latencia)'
  if (phase === 'download') return 'var(--phase-download)'
  if (phase === 'upload') return 'var(--phase-upload)'
  return 'var(--accent)'
}

// Fluxo do PWA (Tela 1 "Velocidade" + Tela 2 "Resultado" do protótipo
// "SignallQ WebApp.dc.html" do Luiz, GH#1186) — uma única rota "/" com
// máquina de estados por fase e chrome mínimo. A publicidade só entra depois
// do resultado: nunca disputa atenção com a medição ou com seu CTA inicial.
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
      {isResult ? (
        <DetailTopBar title="Resultado" onBack={goToIdle} rightIcon="history" rightLabel="Ver histórico" onRightClick={irParaHistorico} />
      ) : (
        <FlowTopBar onHistoryClick={irParaHistorico} />
      )}

      <div
        className={`mx-auto flex w-full flex-1 flex-col items-center gap-4 px-5 pb-6 pt-2 box-border ${
          isResult ? 'max-w-[920px] lg:flex-row lg:items-start lg:justify-center lg:gap-8' : 'max-w-[560px]'
        }`}
      >
        {isIdle && <IdleStart modo={modo} onModoChange={setModo} onIniciar={handleIniciar} />}

        {isRunning && (
          <>
            <div className="max-w-[420px] pt-2.5 text-center body-small">
              Este teste usa dados da sua conexão para medir a velocidade — nenhum valor é simulado.
            </div>
            <SpeedGauge fraction={fraction} color={phaseColor} centerValue={gaugeCenterValue} centerUnit={gaugeCenterUnit} showTicks pulse />
            <div className="overline">{PHASE_LABELS[phase] ?? ''}</div>
            {modo === 'triplo' && round != null && <div className="label-medium">Medição {round} de 3</div>}
            <StepRow steps={steps} />
            <button onClick={cancelTest} className="flex h-10 items-center gap-1.5 border-none bg-transparent">
              <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                close
              </span>
              <span className="label-large">Cancelar teste</span>
            </button>
          </>
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
        {isResult && <AdBanner />}
      </div>
    </div>
  )
}
