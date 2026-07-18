import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';
import { Icon } from '../primitives/Icon.js';
import { Button } from './Button.js';

export interface DialogProps {
  /** Ícone Material Symbols no topo (opcional). */
  icon?: string;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  onConfirm?: () => void;
  onCancel?: () => void;
  children?: React.ReactNode;
}

/** Diálogo MD3 (radius 24) com scrim, sobre um container `position: relative`. */
export function Dialog({ icon, title, description, confirmLabel = 'Confirmar', cancelLabel = 'Cancelar', danger, onConfirm, onCancel, children }: DialogProps) {
  const LK = useTokens();
  return (
    <div style={{ position: 'absolute', inset: 0, background: LK.scrim, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, zIndex: 5 }}>
      <div style={{ background: LK.surfaceContainerHigh, borderRadius: LK.rDialog, padding: 24, width: '100%', maxWidth: 300, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {icon && <Icon name={icon} size={24} color={danger ? LK.error : LK.primary} />}
        <div style={{ font: `600 22px/28px ${LK.font}`, color: LK.onSurface }}>{title}</div>
        {description && <div style={{ font: `400 14px/20px ${LK.font}`, letterSpacing: '.2px', color: LK.onSurfaceVariant }}>{description}</div>}
        {children}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
          <Button variant={danger ? 'danger' : 'filled'} onClick={onConfirm}>{confirmLabel}</Button>
          <Button variant="text" onClick={onCancel}>{cancelLabel}</Button>
        </div>
      </div>
    </div>
  );
}
