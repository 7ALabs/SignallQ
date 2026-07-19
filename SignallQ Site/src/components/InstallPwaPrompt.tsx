import { useState } from 'react'
import { ConfirmDialog } from './ConfirmDialog'
import { usePwaInstall } from '../hooks/usePwaInstall'
import { FEATURE_PWA_INSTALLED, FEATURE_PWA_INSTALL_PROMPTED, trackFeatureUsed } from '../lib/telemetry'

// CTA de instalação discreto — toast que vive dentro do container de pilha
// único `PwaToastStack` (junto com PwaUpdatePrompt), não mais um `fixed`
// independente brigando pelo canto da tela (fix da Lia, ver
// .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md). Em
// Android/Chrome/Edge/desktop, o clique já dispara o prompt nativo do
// navegador; em iOS Safari (sem essa API) abre um dialog leve com a
// instrução manual, no mesmo padrão visual do ConfirmDialog.
export function InstallPwaPrompt() {
  const { visible, canPromptNative, showIosFallback, deferredPrompt, dismiss, markInstalled } = usePwaInstall()
  const [showIosDialog, setShowIosDialog] = useState(false)

  if (!visible) return null

  const handleClick = async () => {
    if (canPromptNative && deferredPrompt) {
      trackFeatureUsed(FEATURE_PWA_INSTALL_PROMPTED)
      await deferredPrompt.prompt()
      const choice = await deferredPrompt.userChoice
      if (choice.outcome === 'accepted') {
        trackFeatureUsed(FEATURE_PWA_INSTALLED)
        markInstalled()
      }
      dismiss()
      return
    }
    if (showIosFallback) {
      trackFeatureUsed(FEATURE_PWA_INSTALL_PROMPTED)
      setShowIosDialog(true)
    }
  }

  return (
    <>
      <div
        className="sq-fade-up flex items-center gap-1.5 rounded-full border py-2 pl-3.5 pr-2"
        style={{ borderColor: 'color-mix(in srgb, var(--border) 40%, transparent)', background: 'var(--bg-card)' }}
      >
        <button onClick={handleClick} className="flex items-center gap-1.5 border-none bg-transparent p-0">
          <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--accent)' }}>
            install_mobile
          </span>
          <span className="label-medium" style={{ color: 'var(--text-primary)' }}>
            Instalar app
          </span>
        </button>
        <button
          onClick={dismiss}
          aria-label="Fechar"
          className="flex h-5 w-5 items-center justify-center border-none bg-transparent p-0"
          style={{ color: 'var(--text-tertiary)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 16 }}>
            close
          </span>
        </button>
      </div>

      {showIosDialog && (
        <ConfirmDialog
          icon="ios_share"
          title="Instalar na tela de início"
          description="Toque em Compartilhar na barra do Safari e escolha “Adicionar à Tela de Início”."
          confirmLabel="Entendi"
          cancelLabel="Fechar"
          onConfirm={() => {
            setShowIosDialog(false)
            dismiss()
          }}
          onCancel={() => setShowIosDialog(false)}
        />
      )}
    </>
  )
}
