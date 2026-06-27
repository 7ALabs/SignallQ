import React from 'react';
import { LK } from '../tokens.js';

export interface SignalBarsProps {
  /** Signal level 1–4 */
  level?: 1 | 2 | 3 | 4;
  /** Bar color for filled levels */
  color?: string;
  /** Larger variant (20dp tall) */
  big?: boolean;
}

/** 4-bar signal-strength glyph matching the Android SignallQ custom icon. */
export function SignalBars({ level = 4, color = LK.success, big = false }: SignalBarsProps) {
  const hs = big ? [7, 11, 15, 20] : [6, 9, 12, 16];
  const w = big ? 4 : 3;
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: w, height: hs[3] }}>
      {hs.map((h, i) => (
        <i
          key={i}
          style={{
            width: w,
            height: h,
            borderRadius: 1,
            display: 'block',
            background: i < level ? color : LK.border,
          }}
        />
      ))}
    </div>
  );
}
