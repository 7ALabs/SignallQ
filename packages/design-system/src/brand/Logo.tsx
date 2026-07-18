import React from 'react';
import { SIGNALLQ_SYMBOL, SIGNALLQ_LOCKUP_LIGHT, SIGNALLQ_LOCKUP_DARK } from './logoData.js';
import { useThemeMode } from '../theme/ThemeProvider.js';

export interface LogoProps {
  /** `'symbol'` = só o símbolo (quadrado); `'lockup'` = símbolo + wordmark. */
  variant?: 'symbol' | 'lockup';
  /** Fundo em que o lockup vai aparecer — usa a versão pré-composta. Sem valor, segue o tema ativo (SignallQThemeProvider). Ignorado para `'symbol'`. */
  background?: 'light' | 'dark';
  /** Altura em px. Símbolo: largura = altura; lockup: largura automática. */
  size?: number;
  style?: React.CSSProperties;
}

/** Marca SignallQ — símbolo e lockup oficiais. Fundação de marca do design system (fonte: `brand/`). */
export function Logo({ variant = 'symbol', background, size = 40, style = {} }: LogoProps) {
  const { resolved } = useThemeMode();
  const isSymbol = variant === 'symbol';
  const src = isSymbol
    ? SIGNALLQ_SYMBOL
    : (background ?? resolved) === 'dark'
      ? SIGNALLQ_LOCKUP_DARK
      : SIGNALLQ_LOCKUP_LIGHT;
  return (
    <img
      src={src}
      alt="SignallQ"
      height={size}
      style={{ display: 'block', height: size, width: isSymbol ? size : 'auto', ...style }}
    />
  );
}
