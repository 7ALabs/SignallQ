import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface ScreenScrollProps {
  children?: React.ReactNode;
}

/** Scrollable screen body with standard padding and vertical gap between sections. */
export function ScreenScroll({ children }: ScreenScrollProps) {
  const LK = useTokens();
  return (
    <div
      style={{
        flex: 1,
        overflowY: 'auto',
        background: LK.bgPrimary,
        padding: '4px 16px 20px',
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
      }}
    >
      {children}
    </div>
  );
}
