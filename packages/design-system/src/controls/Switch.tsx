import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface SwitchProps {
  on?: boolean;
  onChange?: (on: boolean) => void;
}

/** Toggle MD3. */
export function Switch({ on = false, onChange }: SwitchProps) {
  const LK = useTokens();
  return (
    <button
      onClick={() => onChange && onChange(!on)}
      style={{
        width: 44, height: 26, borderRadius: 13, border: on ? 'none' : `2px solid ${LK.outline}`,
        cursor: 'pointer', padding: 0, background: on ? LK.primary : LK.surfaceContainerHighest,
        position: 'relative', flex: 'none', boxSizing: 'border-box',
      }}
    >
      <div style={{ position: 'absolute', top: on ? 3 : 2, left: on ? 21 : 2, width: 20, height: 20, borderRadius: '50%', background: on ? LK.onPrimary : LK.outline }} />
    </button>
  );
}
