import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface TextFieldProps {
  label?: string;
  value?: string;
  placeholder?: string;
  disabled?: boolean;
  onChange?: (value: string) => void;
  style?: React.CSSProperties;
}

/** Campo de texto MD3: label overline + input com borda 1px, radius 12. */
export function TextField({ label, value, placeholder, disabled, onChange, style = {} }: TextFieldProps) {
  const LK = useTokens();
  return (
    <div style={{ marginBottom: 12, ...style }}>
      {label && (
        <div style={{ font: `500 12px/16px ${LK.font}`, letterSpacing: '.3px', color: LK.onSurfaceVariant, marginBottom: 6 }}>{label}</div>
      )}
      <input
        value={value}
        placeholder={placeholder}
        disabled={disabled}
        onChange={(e) => onChange && onChange(e.target.value)}
        style={{
          font: `400 16px/24px ${LK.font}`, color: LK.onSurface, width: '100%', boxSizing: 'border-box',
          border: `1px solid ${LK.outline}`, borderRadius: LK.rField, padding: '12px 14px', background: LK.surface,
          opacity: disabled ? 0.6 : 1,
        }}
      />
    </div>
  );
}
