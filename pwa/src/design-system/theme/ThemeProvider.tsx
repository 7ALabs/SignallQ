import { createContext, ReactNode, useContext } from 'react';

export type ThemeMode = 'light' | 'dark';

const ThemeContext = createContext<ThemeMode>('light');

interface ThemeProviderProps {
  children: ReactNode;
  mode?: ThemeMode;
}

export function ThemeProvider({ children, mode = 'light' }: ThemeProviderProps) {
  return (
    <ThemeContext.Provider value={mode}>
      <div className={['sq-theme', mode === 'dark' ? 'dark' : ''].filter(Boolean).join(' ')} data-theme={mode}>
        {children}
      </div>
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
