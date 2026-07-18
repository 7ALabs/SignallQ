import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';
import { Icon } from '../primitives/Icon.js';

export type ButtonVariant = 'filled' | 'tonal' | 'outlined' | 'text' | 'danger';

export interface ButtonProps {
  variant?: ButtonVariant;
  /** Nome do ícone Material Symbols à esquerda do label. */
  icon?: string;
  disabled?: boolean;
  fullWidth?: boolean;
  onClick?: () => void;
  children?: React.ReactNode;
  style?: React.CSSProperties;
}

/** Botão MD3: filled / tonal / outlined / text / danger. Altura 40, radius 20. */
export function Button({ variant = 'filled', icon, disabled, fullWidth = true, onClick, children, style = {} }: ButtonProps) {
  const LK = useTokens();
  const base: React.CSSProperties = {
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    height: 40, borderRadius: LK.rBtn, padding: '0 24px', border: 0,
    font: `500 14px/20px ${LK.font}`, letterSpacing: '.1px',
    cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.38 : 1,
    width: fullWidth ? '100%' : 'auto', boxSizing: 'border-box',
  };
  let color = LK.onPrimary as string;
  if (variant === 'filled') { base.background = LK.primary; color = LK.onPrimary; }
  else if (variant === 'tonal') { base.background = LK.secondaryContainer; color = LK.onSecondaryContainer; }
  else if (variant === 'outlined') { base.background = 'transparent'; color = LK.primary; base.border = `1px solid ${LK.outline}`; }
  else if (variant === 'text') { base.background = 'transparent'; color = LK.primary; base.padding = '0 12px'; }
  else if (variant === 'danger') { base.background = LK.error; color = LK.onError; }
  base.color = color;
  return (
    <button onClick={onClick} disabled={disabled} style={{ ...base, ...style }}>
      {icon && <Icon name={icon} size={18} color={color} />}
      {children}
    </button>
  );
}
