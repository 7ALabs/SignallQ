// Banner "Nova versão disponível" do Service Worker.
//
// Estratégia (resolve o ciclo de update conservador do Safari/iOS), portada
// do repo antecessor linka-speedtest:
//   1. `registerType: 'autoUpdate'` + `skipWaiting`/`clientsClaim` no
//      vite.config.ts — o novo SW assume controle imediatamente.
//   2. Verificação periódica a cada 60s via `registration.update()` —
//      garante que o navegador re-cheque o service worker mesmo sem reload.
//   3. UX explícita: ao detectar nova versão, mostra este banner com
//      "Atualizar" (força reload) ou "Fechar" (adia até a próxima visita).
//
// Sem reload-surpresa: o usuário escolhe quando aplicar a atualização.
//
// Casca visual unificada com InstallPwaPrompt (bg-card + borda + pill, ícone
// Material Symbol único, nunca bloco `--accent` sólido) — os dois vivem
// dentro do `PwaToastStack` (fix da Lia, ver
// .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md). Antes disso
// este componente tinha seu próprio CSS com bloco de cor sólida e o
// glifo "×" cru — removido.
import { useRegisterSW } from 'virtual:pwa-register/react'

const UPDATE_CHECK_INTERVAL_MS = 60_000

export function PwaUpdatePrompt() {
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW({
    onRegistered(registration) {
      if (!registration) return
      setInterval(() => {
        registration.update().catch(() => {
          // offline ou falha transiente — próxima verificação tenta de novo
        })
      }, UPDATE_CHECK_INTERVAL_MS)
    },
    onRegisterError(error) {
      console.warn('[pwa] falha ao registrar service worker:', error)
    },
  })

  if (!needRefresh) return null

  return (
    <div
      className="sq-fade-up flex items-center gap-1.5 rounded-full border py-2 pl-3.5 pr-2"
      style={{ borderColor: 'color-mix(in srgb, var(--border) 40%, transparent)', background: 'var(--bg-card)' }}
      role="status"
      aria-live="polite"
    >
      <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--accent)' }}>
        sync
      </span>
      <span className="label-medium" style={{ color: 'var(--text-primary)' }}>
        Nova versão disponível
      </span>
      <button
        type="button"
        onClick={() => {
          void updateServiceWorker(true)
        }}
        className="border-none bg-transparent p-0"
      >
        <span className="label-medium" style={{ color: 'var(--accent)' }}>
          Atualizar
        </span>
      </button>
      <button
        type="button"
        onClick={() => setNeedRefresh(false)}
        aria-label="Fechar"
        className="flex h-5 w-5 items-center justify-center border-none bg-transparent p-0"
        style={{ color: 'var(--text-tertiary)' }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 16 }}>
          close
        </span>
      </button>
    </div>
  )
}
