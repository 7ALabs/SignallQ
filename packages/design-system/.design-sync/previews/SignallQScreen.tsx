import React from 'react';
import { SignallQScreen, PhoneFrame, StatusBar, LK } from '@signallq/design-system';

export const InFrame = () => (
  <div style={{ padding: 24, background: '#F0F0F0', display: 'inline-block', fontFamily: LK.font }}>
    <PhoneFrame>
      <StatusBar />
      <SignallQScreen />
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
    }}
  >
    <SignallQScreen />
  </div>
);
