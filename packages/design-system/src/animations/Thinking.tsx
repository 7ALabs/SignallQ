import React from 'react';
import { ORB } from '../tokens.js';
import { useTokens } from '../theme/ThemeProvider.js';

export interface ThinkingProps {
  style?: React.CSSProperties;
}

/** Three-dot pulsing animation shown while SignallQ AI is processing. */
export function Thinking({ style }: ThinkingProps) {
  const LK = useTokens();
  return (
    <>
      <style>{`
        @keyframes orbPulse {
          0%, 100% { opacity: 0.3; transform: scale(0.8); }
          50% { opacity: 1; transform: scale(1); }
        }
      `}</style>
      <div
        style={{
          display: 'flex',
          gap: 5,
          padding: '14px 16px',
          background: ORB.card,
          borderRadius: 16,
          width: 'fit-content',
          ...style,
        }}
      >
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            style={{
              width: 7,
              height: 7,
              borderRadius: '50%',
              background: LK.accent,
              animation: `orbPulse 1s ${i * 0.15}s infinite ease-in-out`,
              display: 'block',
            }}
          />
        ))}
      </div>
    </>
  );
}
