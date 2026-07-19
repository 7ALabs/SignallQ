import { InstallPwaPrompt } from './InstallPwaPrompt'
import { PwaUpdatePrompt } from './PwaUpdatePrompt'

// Pilha coordenada dos dois toasts fixos do PWA (atualizar + instalar) — cada
// um era antes um elemento `fixed` independente brigando pelo mesmo canto da
// tela (instalar em bottom-4 left-4, atualizar em bottom-center quase
// full-width — achado da Lia, ver
// .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md). Agora os dois
// vivem na mesma casca visual dentro de um único container fixo,
// column-reverse com gap fixo — nunca dois `fixed` disputando espaço.
export function PwaToastStack() {
  return (
    <div
      className="fixed inset-x-0 bottom-4 z-[1000] flex flex-col-reverse items-center gap-2 px-4"
      style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}
    >
      <PwaUpdatePrompt />
      <InstallPwaPrompt />
    </div>
  )
}
