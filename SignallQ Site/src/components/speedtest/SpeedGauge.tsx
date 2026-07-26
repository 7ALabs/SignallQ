import {
  GAUGE_ARC_LEN,
  GAUGE_ARC_PATH,
  GAUGE_NEEDLE_FROM_R,
  GAUGE_NEEDLE_TO_R,
  GAUGE_SCALE_LABELS,
  GAUGE_TICKS,
  pointOnArc,
} from '../../lib/gaugeMath'

interface SpeedGaugeIdleProps {
  variant: 'idle'
  onIniciar: () => void
}

interface SpeedGaugeRunningProps {
  variant: 'running'
  fraction: number
  color: string
  /** Ícone Material do rótulo da fase ativa (ex.: `arrow_downward`, `arrow_upward`, `network_ping`). */
  phaseIcon: string
  /** Rótulo da fase ativa (ex.: `Download`, `Upload`, `Latência`). */
  phaseLabel: string
  centerValue: string
  centerUnit: string
}

type SpeedGaugeProps = SpeedGaugeIdleProps | SpeedGaugeRunningProps

// Velocímetro (arco SVG 360x210) — reconstrução v2 (`ScreenHome.dc.html`). Diferente da
// versão anterior, o CTA "Iniciar teste" (idle) e o readout de fase (running) agora vivem
// DENTRO do próprio mostrador, não como elementos separados abaixo — 1:1 com o protótipo.
export function SpeedGauge(props: SpeedGaugeProps) {
  const isRunning = props.variant === 'running'
  const fraction = isRunning ? props.fraction : 0
  const needle = pointOnArc(GAUGE_NEEDLE_TO_R, fraction)
  const needleFrom = pointOnArc(GAUGE_NEEDLE_FROM_R, fraction)

  return (
    <div className="relative w-full lg:w-[440px]" style={{ aspectRatio: '360 / 210' }}>
      <svg viewBox="0 0 360 210" className="absolute inset-0 block h-full w-full">
        {GAUGE_TICKS.map((t, i) => (
          <line
            key={i}
            x1={t.x1}
            y1={t.y1}
            x2={t.x2}
            y2={t.y2}
            stroke={t.major ? 'color-mix(in srgb, var(--text-tertiary) 45%, transparent)' : 'color-mix(in srgb, var(--border) 28%, transparent)'}
            strokeWidth={t.major ? 2 : 1}
            strokeLinecap="round"
          />
        ))}
        <path d={GAUGE_ARC_PATH} stroke="color-mix(in srgb, var(--border) 20%, transparent)" strokeWidth={12} fill="none" strokeLinecap="round" />
        {isRunning && (
          <>
            <path
              d={GAUGE_ARC_PATH}
              stroke={props.color}
              strokeWidth={12}
              fill="none"
              strokeLinecap="round"
              strokeDasharray={GAUGE_ARC_LEN}
              strokeDashoffset={GAUGE_ARC_LEN * (1 - fraction)}
              style={{ transition: 'stroke-dashoffset 250ms ease-out' }}
            />
            <line
              x1={needleFrom.x}
              y1={needleFrom.y}
              x2={needle.x}
              y2={needle.y}
              stroke={props.color}
              strokeWidth={4}
              strokeLinecap="round"
              style={{ transition: 'x2 250ms ease-out, y2 250ms ease-out' }}
            />
          </>
        )}
      </svg>

      {GAUGE_SCALE_LABELS.map((l) => (
        <div
          key={l.text}
          className="label-medium absolute"
          style={{ left: `${l.leftPercent}%`, top: `${l.topPercent}%`, transform: 'translate(-50%, -50%)', color: 'var(--text-tertiary)' }}
        >
          {l.text}
        </div>
      ))}

      {!isRunning && (
        <div className="absolute bottom-[26px] left-1/2 flex -translate-x-1/2 flex-col items-center gap-2.5">
          <button
            onClick={props.onIniciar}
            aria-label="Iniciar teste"
            className="flex h-11 items-center gap-2 whitespace-nowrap rounded-full border-none px-[26px]"
            style={{ background: 'var(--accent)', boxShadow: '0 10px 22px color-mix(in srgb, var(--accent) 28%, transparent)' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--on-accent)' }}>
              speed
            </span>
            <span style={{ font: '600 16px/1.15 var(--font-sans)', color: 'var(--on-accent)' }}>Iniciar teste</span>
          </button>
          <span className="body-small" style={{ color: 'var(--text-tertiary)' }}>
            Rápido · ~20 s
          </span>
        </div>
      )}

      {isRunning && (
        <div className="absolute inset-x-0 bottom-1 flex flex-col items-center">
          <div className="flex items-center gap-1.5">
            <span className="material-symbols-outlined" style={{ fontSize: 18, color: props.color }}>
              {props.phaseIcon}
            </span>
            <span className="overline">{props.phaseLabel}</span>
          </div>
          <div
            className="text-[44px] font-bold leading-none lg:text-[54px]"
            style={{ fontFamily: 'var(--font-sans)', color: 'var(--text-primary)' }}
          >
            {props.centerValue}
          </div>
          <div className="label-large" style={{ color: 'var(--text-secondary)' }}>
            {props.centerUnit}
          </div>
        </div>
      )}
    </div>
  )
}
