import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface SegmentedControlProps {
  options: string[];
  value: string;
  onChange?: (value: string) => void;
  style?: React.CSSProperties;
}

/** Seletor segmentado MD3 (2-3 opções), pill com opção ativa em secondaryContainer. */
export function SegmentedControl({ options, value, onChange, style = {} }: SegmentedControlProps) {
  const LK = useTokens();
  return (
    <div style={{ display: 'flex', border: `1px solid ${LK.outline}`, borderRadius: 20, padding: 2, gap: 2, ...style }}>
      {options.map((opt) => {
        const on = opt === value;
        return (
          <button
            key={opt}
            onClick={() => onChange && onChange(opt)}
            style={{
              font: `500 14px/20px ${LK.font}`, letterSpacing: '.1px', flex: 1, border: 0, cursor: 'pointer',
              padding: '9px 0', borderRadius: 18,
              background: on ? LK.secondaryContainer : 'transparent',
              color: on ? LK.onSecondaryContainer : LK.onSurfaceVariant,
            }}
          >
            {opt}
          </button>
        );
      })}
    </div>
  );
}
