import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';
import { Icon } from '../primitives/Icon.js';

export interface IconButtonProps {
  name: string;
  color?: string;
  onClick?: () => void;
  style?: React.CSSProperties;
}

/** Botão só de ícone, circular 40dp. */
export function IconButton({ name, color, onClick, style = {} }: IconButtonProps) {
  const LK = useTokens();
  return (
    <button
      onClick={onClick}
      style={{ width: 40, height: 40, borderRadius: 20, background: 'none', border: 0, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', ...style }}
    >
      <Icon name={name} size={22} color={color ?? LK.onSurfaceVariant} />
    </button>
  );
}
