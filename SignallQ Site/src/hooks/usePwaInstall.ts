// Estado de instalabilidade do PWA — captura o evento `beforeinstallprompt`
// (Android/Chrome/Edge/desktop) e detecta o caso iOS Safari, que nunca dispara
// esse evento (não existe API nativa de instalação lá).
import { useCallback, useEffect, useState } from 'react'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

const DISMISSED_KEY = 'signallq_pwa_install_dismissed'

function isIos(): boolean {
  if (typeof navigator === 'undefined') return false
  return /iphone|ipad|ipod/i.test(navigator.userAgent) && !('MSStream' in window)
}

function isStandalone(): boolean {
  if (typeof window === 'undefined') return false
  const nav = navigator as Navigator & { standalone?: boolean }
  return window.matchMedia?.('(display-mode: standalone)').matches === true || nav.standalone === true
}

function readDismissed(): boolean {
  try {
    return localStorage.getItem(DISMISSED_KEY) === '1'
  } catch {
    return false
  }
}

export function usePwaInstall() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [installed, setInstalled] = useState(() => isStandalone())
  const [dismissed, setDismissed] = useState(readDismissed)

  useEffect(() => {
    if (installed) return
    const onBeforeInstall = (e: Event) => {
      e.preventDefault()
      setDeferredPrompt(e as BeforeInstallPromptEvent)
    }
    const onInstalled = () => {
      setInstalled(true)
      setDeferredPrompt(null)
    }
    window.addEventListener('beforeinstallprompt', onBeforeInstall)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [installed])

  const dismiss = useCallback(() => {
    setDismissed(true)
    try {
      localStorage.setItem(DISMISSED_KEY, '1')
    } catch {
      // localStorage indisponível — fecha só nesta sessão
    }
  }, [])

  const markInstalled = useCallback(() => setInstalled(true), [])

  const canPromptNative = deferredPrompt !== null
  const showIosFallback = isIos() && !installed
  const visible = !installed && !dismissed && (canPromptNative || showIosFallback)

  return { visible, canPromptNative, showIosFallback, deferredPrompt, dismiss, markInstalled }
}
