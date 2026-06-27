import React from 'react';
import { LK } from '../tokens.js';

export interface OverlineProps {
  children?: React.ReactNode;
  style?: React.CSSProperties;
}

/** Section label: 11px semibold, tertiary color, UPPERCASE with letter-spacing. */
export function Overline({ children, style }: OverlineProps) {
  return (
    <div
      style={{
        font: `600 11px/1.3 ${LK.font}`,
        color: LK.textTertiary,
        letterSpacing: '.4px',
        textTransform: 'uppercase',
        ...style,
      }}
    >
      {children}
    </div>
  );
}
