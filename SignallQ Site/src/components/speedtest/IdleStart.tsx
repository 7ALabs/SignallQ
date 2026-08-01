import Link from 'next/link'
import type { SpeedTestMode } from '../../lib/speedEngine'
import { SegmentedControl } from '../SegmentedControl'
import { InfraInfoCard } from './InfraInfoCard'
import { SpeedGauge } from './SpeedGauge'

// Alias de `SpeedTestMode` (definido em speedEngine.ts, fonte única do
// contrato Rápido/Completo desde GH#1367) — mantido com este nome aqui porque
// é o vocabulário já usado pelos consumidores desta tela (`modo`/`onModoChange`).
export type ModoTeste = SpeedTestMode

interface IdleStartProps {
  modo: ModoTeste
  onModoChange: (modo: ModoTeste) => void
  onIniciar: () => void
}

const MODOS: Array<{ value: ModoTeste; label: string }> = [
  { value: 'rapido', label: 'Rápido' },
  { value: 'completo', label: 'Completo' },
  { value: 'triplo', label: '3 testes' },
]

// Tela idle do teste de velocidade — reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHome.dc.html`). O CTA
// "Iniciar teste" deixou de ser um botão avulso e agora vive dentro do próprio
// velocímetro (`SpeedGauge` variant="idle") — 1:1 com o protótipo.
export function IdleStart({ modo, onModoChange, onIniciar }: IdleStartProps) {
  return (
    <div className="flex w-full flex-col items-center gap-5">
      <div className="flex flex-col items-center gap-1.5 text-center">
        <h1
          className="m-0 text-[26px] font-bold leading-[1.2] lg:text-[30px]"
          style={{ fontFamily: 'var(--font-sans)', color: 'var(--text-primary)' }}
        >
          Teste de velocidade
        </h1>
        <p className="m-0 max-w-[460px] body-medium" style={{ color: 'var(--text-secondary)' }}>
          Download, upload e latência medidos no seu navegador.
        </p>
      </div>

      <SpeedGauge variant="idle" onIniciar={onIniciar} />

      <InfraInfoCard />

      <div className="flex w-full max-w-[300px]">
        <SegmentedControl options={MODOS} value={modo} onChange={onModoChange} />
      </div>

      <Link href="/como-medimos" className="label-medium no-underline" style={{ color: 'var(--accent)' }}>
        Como medimos sua conexão
      </Link>
    </div>
  )
}


