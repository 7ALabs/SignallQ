import { Link } from 'react-router-dom'
import type { SpeedTestMode } from '../../lib/speedEngine'
import { SegmentedControl } from '../SegmentedControl'

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

// Tela idle do teste de velocidade — protótipo "SignallQ WebApp.dc.html" do
// Luiz (GH#1186), substitui o auto-start anterior (o motor só rodava sozinho
// ao abrir "/"). "Rápido" x "Completo" diferenciam de fato o rigor da medição
// desde GH#1367 — ver `SPEED_TEST_MODE_CONFIG` em speedEngine.ts.
//
export function IdleStart({ modo, onModoChange, onIniciar }: IdleStartProps) {
  return (
    <div className="flex flex-col items-center gap-6 pt-8">
      <button
        onClick={onIniciar}
        aria-label="Iniciar teste"
        className="flex items-center justify-center rounded-full border-none p-0 text-center"
        style={{
          width: 192,
          height: 192,
          background: 'var(--accent)',
          boxShadow: '0 0 0 14px color-mix(in srgb, var(--accent) 35%, transparent)',
        }}
      >
        <span style={{ color: '#fff', fontSize: 26, fontWeight: 600, lineHeight: 1.15 }}>Iniciar teste</span>
      </button>

      <div className="w-full max-w-[300px]">
        <SegmentedControl options={MODOS} value={modo} onChange={onModoChange} />
      </div>

      <div className="body-small text-center" style={{ color: 'var(--text-tertiary)' }}>
        Infraestrutura de medição: rede Cloudflare
      </div>

      <Link to="/como-medimos" className="label-medium no-underline" style={{ color: 'var(--accent)' }}>
        Como medimos sua conexão
      </Link>
    </div>
  )
}
