import React from 'react';
import { SIGNALLQ_SYMBOL } from './logoData.js';

export interface LogoProps {
  /** Tamanho (largura = altura) do símbolo, em px. */
  size?: number;
  style?: React.CSSProperties;
}

/** Marca SignallQ — símbolo oficial. Fundação de marca do design system (fonte: `brand/`). */
export function Logo({ size = 40, style = {} }: LogoProps) {
  return (
    <img
      src={SIGNALLQ_SYMBOL}
      alt="SignallQ"
      width={size}
      height={size}
      style={{ display: 'block', width: size, height: size, ...style }}
    />
  );
}
