import React from 'react';
import { Thinking, ORB, LK } from '@signallq/design-system';

export const Default = () => (
  <div style={{ background: ORB.bg, padding: 24, fontFamily: LK.font }}>
    <div style={{ display: 'flex', gap: 10 }}>
      <div
        style={{
          width: 28,
          height: 28,
          borderRadius: '50%',
          background: `linear-gradient(135deg, ${LK.accent}, ${LK.accentBlue})`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flex: 'none',
        }}
      />
      <Thinking />
    </div>
  </div>
);
