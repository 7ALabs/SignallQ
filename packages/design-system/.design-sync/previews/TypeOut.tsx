import React from 'react';
import { TypeOut, ORB, LK } from '@signallq/design-system';

export const AIResponse = () => (
  <div style={{ background: ORB.bg, padding: 24, fontFamily: LK.font, maxWidth: 320 }}>
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
          marginTop: 2,
        }}
      />
      <div
        style={{
          background: ORB.card,
          borderRadius: '4px 16px 16px 16px',
          padding: '13px 15px',
          font: `400 14px/1.55 ${LK.font}`,
          color: ORB.text,
        }}
      >
        <TypeOut text="Sua internet está com ótima velocidade (486 Mbps), mas notei uma pequena oscilação de latência — 24 ms de jitter." speed={30} />
      </div>
    </div>
  </div>
);
