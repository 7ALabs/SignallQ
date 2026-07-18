import React, { createContext, useContext, useEffect, useState } from 'react';
import { LK, LK_DARK, type TokenSet } from '../tokens.js';

export type ThemeMode = 'light' | 'dark' | 'system';

const TokensContext = createContext<TokenSet>(LK);
const ModeContext = createContext<{ mode: ThemeMode; resolved: 'light' | 'dark' }>({
  mode: 'light',
  resolved: 'light',
});

function systemPrefersDark(): boolean {
  return typeof window !== 'undefined' && !!window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

export interface SignallQThemeProviderProps {
  /** `'light'` | `'dark'` | `'system'` (default) — `'system'` acompanha `prefers-color-scheme`. */
  mode?: ThemeMode;
  children?: React.ReactNode;
}

/** Provedor de tema do design system — resolve LK (claro) ou LK_DARK (escuro) e disponibiliza via useTokens(). */
export function SignallQThemeProvider({ mode = 'system', children }: SignallQThemeProviderProps) {
  const [systemDark, setSystemDark] = useState(systemPrefersDark);

  useEffect(() => {
    if (mode !== 'system' || typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => setSystemDark(mq.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [mode]);

  const resolved: 'light' | 'dark' = mode === 'dark' || (mode === 'system' && systemDark) ? 'dark' : 'light';
  const tokens = resolved === 'dark' ? LK_DARK : LK;

  return (
    <ModeContext.Provider value={{ mode, resolved }}>
      <TokensContext.Provider value={tokens}>{children}</TokensContext.Provider>
    </ModeContext.Provider>
  );
}

/** Tokens ativos (LK ou LK_DARK). Funciona também fora de um SignallQThemeProvider — retorna LK (claro). */
export function useTokens(): TokenSet {
  return useContext(TokensContext);
}

/** Modo configurado no provedor mais próximo e o tema resolvido ('light' | 'dark'). */
export function useThemeMode(): { mode: ThemeMode; resolved: 'light' | 'dark' } {
  return useContext(ModeContext);
}
