import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface ChipProps {
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  children?: React.ReactNode;
}

/** Chip (filtro/seleção) MD3, pill 999. */
export function Chip({ active, disabled, onClick, children }: ChipProps) {
  const LK = useTokens();
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      style={{
        font: `500 14px/20px ${LK.font}`, letterSpacing: '.1px', border: 0, cursor: disabled ? 'default' : 'pointer',
        padding: '8px 16px', borderRadius: LK.rPill,
        background: active ? LK.secondaryContainer : LK.surfaceContainerHigh,
        color: active ? LK.onSecondaryContainer : LK.onSurfaceVariant,
        opacity: disabled && !active ? 0.6 : 1,
      }}
    >
      {children}
    </button>
  );
}
