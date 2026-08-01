import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { IdleStart, type ModoTeste } from './IdleStart'
import { ProblemPanel } from './ProblemPanel'
import { ResultPanel } from './ResultPanel'
import { RunningPanel } from './RunningPanel'
import type { StepInfo } from './StepRow'
import { type FasePainel, type ProblemPhase, useSpeedTest } from '../../hooks/useSpeedTest'
import { classifyDownload } from '../../lib/classification'
import { fractionForLatency, fractionForThroughput } from '../../lib/gaugeMath'

const RUNNING_PHASES: FasePainel[] = ['preparando', 'latencia', 'download', 'upload', 'processando']
const PROBLEM_PHASES: ProblemPhase[] = ['sem-conexao', 'conexao-interrompida', 'endpoint-indisponivel', 'erro-inesperado', 'cancelado', 'bloqueado-outra-aba']
const STEP_ORDER: Array<'latencia' | 'download' | 'upload'> = ['latencia', 'download', 'upload']
const STEP_LABELS: Record<'latencia' | 'download' | 'upload', string> = { latencia: 'Latência', download: 'Download', upload: 'Upload' }
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

export function EmbeddedSpeedTest() {
  const router = useRouter(); const navigate = (p: string) => router.push(p)
  const [modo, setModo] = useState<ModoTeste>('rapido')
  const { phase, liveValue, phaseResults, result, connectionKind, round, cancelTest, retry, forceStart } = useSpeedTest(modo)

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

  const irParaHistorico = () => navigate('/historico')

  return (
    <div className={`flex w-full flex-col items-center gap-5 ${isResult ? 'max-w-[620px]' : 'max-w-[860px]'} mx-auto`}>
      {isIdle && <IdleStart modo={modo} onModoChange={setModo} onIniciar={forceStart} />}

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
    </div>
  )
}

