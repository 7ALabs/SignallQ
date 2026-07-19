import { useEffect, useState } from 'react'

// Detecta e acompanha o tema claro/escuro do sistema, aplicando `.dark` na
// raiz (os tokens CSS var(--...) já reagem a essa classe — ver tokens.css).
export function useSystemTheme(): boolean {
  const [isDark, setIsDark] = useState(
    () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
  )

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () => {
      document.documentElement.classList.toggle('dark', mq.matches)
      setIsDark(mq.matches)
    }
    apply()
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [])

  return isDark
}
