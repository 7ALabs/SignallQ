import { InfraInfoCard } from './InfraInfoCard'
import { SpeedGauge } from './SpeedGauge'
import { StepRow, type StepInfo } from './StepRow'

interface RunningPanelProps {
  fraction: number
  color: string
  phaseIcon: string
  phaseLabel: string
  centerValue: string
  centerUnit: string
  round: number | null
  steps: StepInfo[]
  onCancel: () => void
}

// Estado "running" do teste de velocidade — reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHome.dc.html`). Gauge com
// readout de fase embutido + card de infraestrutura (compartilhado com o idle) + grade
// de 3 métricas (Latência/Download/Upload) + cancelar.
export function RunningPanel({ fraction, color, phaseIcon, phaseLabel, centerValue, centerUnit, round, steps, onCancel }: RunningPanelProps) {
  return (
    <div className="flex w-full flex-col items-center gap-5">
      <SpeedGauge
        variant="running"
        fraction={fraction}
        color={color}
        phaseIcon={phaseIcon}
        phaseLabel={phaseLabel}
        centerValue={centerValue}
        centerUnit={centerUnit}
      />

      <InfraInfoCard />

      {round != null && <div className="label-medium">Medição {round} de 3</div>}

      <StepRow steps={steps} />

      <button onClick={onCancel} className="flex h-10 items-center gap-1.5 border-none bg-transparent">
        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
          close
        </span>
        <span className="label-large">Cancelar teste</span>
      </button>
    </div>
  )
}
