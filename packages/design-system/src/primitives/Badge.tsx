import React from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';

export interface BadgeProps {
  children: React.ReactNode;
  /** Semantic color — defaults to accent violet */
  color?: string;
  /** Background override — defaults to color at 12% alpha */
  bg?: string;
  style?: React.CSSProperties;
}

/** Inline pill chip with semantic color tint. Used for status labels ("Conectado", verdicts). */
export function Badge({ children, color = LK.accent, bg, style = {} }: BadgeProps) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        font: `600 11px/1 ${LK.font}`,
        color,
        background: bg ?? hexA(color, 0.12),
        padding: '5px 9px',
        borderRadius: 999,
        whiteSpace: 'nowrap',
        ...style,
      }}
    >
      {children}
    </span>
  );
}
