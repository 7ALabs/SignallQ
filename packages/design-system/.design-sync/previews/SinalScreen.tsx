import React from 'react';
import { SinalScreen, PhoneFrame, StatusBar, TopBar, BottomNav, LK } from '@signallq/design-system';

export const InFrame = () => (
  <div style={{ padding: 24, background: '#F0F0F0', display: 'inline-block', fontFamily: LK.font }}>
    <PhoneFrame>
      <StatusBar />
      <TopBar title="Sinal" icon="settings_input_antenna" />
      <SinalScreen />
      <BottomNav active="sinal" />
    </PhoneFrame>
  </div>
);

export const Standalone = () => (
  <div
    style={{
      width: 390,
      height: 560,
      display: 'flex',
      flexDirection: 'column',
      background: LK.bgPrimary,
      fontFamily: LK.font,
    }}
  >
    <SinalScreen />
  </div>
);
