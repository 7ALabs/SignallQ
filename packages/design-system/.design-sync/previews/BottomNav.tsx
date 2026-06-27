import React from 'react';
import { BottomNav, LK } from '@signallq/design-system';

export const HomeActive = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <BottomNav active="home" />
  </div>
);

export const SpeedActive = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <BottomNav active="speed" />
  </div>
);

export const SinalActive = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <BottomNav active="sinal" />
  </div>
);
