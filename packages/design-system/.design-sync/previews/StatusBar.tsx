import React from 'react';
import { StatusBar, LK } from '@signallq/design-system';

export const Default = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <StatusBar />
  </div>
);

export const CustomTime = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <StatusBar time="09:41" />
  </div>
);
