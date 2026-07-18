import React from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';

export interface SheetFrameProps {
  children?: React.ReactNode;
  style?: React.CSSProperties;
}

/** Bottom-sheet chrome: superfície baixa, cantos superiores 28dp, alça (grab handle) e conteúdo rolável. */
export function SheetFrame({ children, style = {} }: SheetFrameProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        background: LK.bgSecondary,
        borderTopLeftRadius: LK.rSheet,
        borderTopRightRadius: LK.rSheet,
        overflow: 'hidden',
        flex: '1 1 auto',
        minHeight: 0,
        boxSizing: 'border-box',
        ...style,
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'center', padding: '12px 0 4px', flex: 'none' }}>
        <div style={{ width: 32, height: 4, borderRadius: 2, background: hexA(LK.border, 0.4) }} />
      </div>
      <div style={{ padding: '20px 24px 32px', overflowY: 'auto', flex: '1 1 auto', minHeight: 0 }}>
        {children}
      </div>
    </div>
  );
}
