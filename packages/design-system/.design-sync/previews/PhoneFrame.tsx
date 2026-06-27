import React from 'react';
import { PhoneFrame, StatusBar, TopBar, BottomNav, LK } from '@signallq/design-system';

export const EmptyShell = () => (
  <div style={{ padding: 24, background: '#F8F8F8', display: 'inline-block', fontFamily: LK.font }}>
    <PhoneFrame>
      <StatusBar />
      <TopBar title="Início" />
      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: LK.bgPrimary,
        }}
      >
        <div style={{ font: `400 14px/1 ${LK.font}`, color: LK.textTertiary }}>Conteúdo da tela</div>
      </div>
      <BottomNav active="home" />
    </PhoneFrame>
  </div>
);
