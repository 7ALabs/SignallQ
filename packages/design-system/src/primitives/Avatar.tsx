import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';

export interface AvatarProps {
  /** Diameter in dp */
  size?: number;
  /** Single letter displayed in the avatar */
  letter?: string;
}

/** Circular gradient avatar (accent → blue). Used in the top bar leading slot. */
export function Avatar({ size = 44, letter = 'L' }: AvatarProps) {
  const LK = useTokens();
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: `linear-gradient(135deg, ${LK.accent}, ${LK.accentBlue})`,
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        font: `700 ${size * 0.4}px/1 ${LK.font}`,
        flex: 'none',
      }}
    >
      {letter}
    </div>
  );
}
