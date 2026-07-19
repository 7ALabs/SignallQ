import { Link } from 'react-router-dom'
import { SegmentedControl } from '../SegmentedControl'

export type ModoTeste = 'rapido' | 'completo' | 'pro'

interface IdleStartProps {
  modo: ModoTeste
  onModoChange: (modo: ModoTeste) => void
  onIniciar: () => void
}

const MODOS: Array<{ value: ModoTeste; label: string }> = [
  { value: 'rapido', label: 'Rápido' },
  { value: 'completo', label: 'Completo' },
  { value: 'pro', label: 'PRO' },
]

// Tela idle do teste de velocidade — protótipo "SignallQ WebApp.dc.html" do
// Luiz (GH#1186), substitui o auto-start anterior (o motor só rodava sozinho
// ao abrir "/"). "Completo" hoje dispara o mesmo motor de "Rápido" — o
// speedEngine.ts não tem parâmetro de duração/rounds variável ainda;
// diferenciação real fica pra outra tarefa, sinalizado no TODO abaixo em vez
// de fingir um parâmetro que não faz nada. "PRO" não roda teste nenhum: só
// aponta pro app SignallQ PRO (decisão de produto minha, não vinha
// especificada linha a linha no handoff — sinalizada no resumo da entrega).
export function IdleStart({ modo, onModoChange, onIniciar }: IdleStartProps) {
  return (
    <div className="flex flex-col items-center gap-6 pt-8">
      <button
        onClick={onIniciar}
        aria-label="Iniciar teste"
        className="flex items-center justify-center rounded-full border-none p-0 text-center"
        style={{ width: 192, height: 192, background: '#5B21D6', boxShadow: '0 0 0 14px rgba(91,33,214,0.35)' }}
      >
        <span style={{ color: '#fff', fontSize: 26, fontWeight: 600, lineHeight: 1.15 }}>Iniciar teste</span>
      </button>

      {/* TODO: "Completo" ainda dispara o mesmo motor de "Rápido" —
          speedEngine.ts não expõe parâmetro de duração/rounds variável.
          Diferenciação real de duração/rounds é tarefa futura, não inventada aqui. */}
      <div className="w-full max-w-[300px]">
        <SegmentedControl options={MODOS} value={modo} onChange={onModoChange} />
      </div>

      <div className="body-small text-center" style={{ color: 'var(--text-tertiary)' }}>
        Servidor mais próximo · detectado automaticamente
      </div>

      {modo === 'pro' && (
        <div
          className="max-w-[320px] rounded-xl border p-3.5 text-center"
          style={{ borderColor: 'color-mix(in srgb, var(--accent) 30%, transparent)', background: 'color-mix(in srgb, var(--accent) 8%, transparent)' }}
        >
          <div className="body-small">
            Recursos PRO estão no app{' '}
            <Link to="/pro" className="label-large" style={{ color: 'var(--accent)' }}>
              SignallQ PRO
            </Link>
            .
          </div>
        </div>
      )}
    </div>
  )
}
