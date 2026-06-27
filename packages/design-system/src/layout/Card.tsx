import React from 'react';
import { LK } from '../tokens.js';

export interface CardProps {
  children?: React.ReactNode;
  style?: React.CSSProperties;
  onClick?: () => void;
}

/** Surface card: white background, 1px border, 16dp radius, flat (no shadow). */
export function Card({ children, style = {}, onClick }: CardProps) {
  return (
    <div
      onClick={onClick}
      style={{
        background: LK.bgCard,
        border: `1px solid ${LK.border}`,
        borderRadius: LK.rCard,
        padding: 16,
        boxSizing: 'border-box',
        cursor: onClick ? 'pointer' : 'default',
        ...style,
      }}
    >
      {children}
    </div>
  );
}
