import React from 'react';
import { SignalBars, LK } from '@signallq/design-system';

export const AllLevels = () => (
  <div style={{ display: 'flex', gap: 24, padding: 20, alignItems: 'flex-end', fontFamily: LK.font }}>
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
      <SignalBars level={4} color={LK.success} />
      <span style={{ font: `600 10px/1 ${LK.font}`, color: LK.success }}>Forte</span>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
      <SignalBars level={3} color={LK.success} />
      <span style={{ font: `600 10px/1 ${LK.font}`, color: LK.success }}>Bom</span>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
      <SignalBars level={2} color={LK.warning} />
      <span style={{ font: `600 10px/1 ${LK.font}`, color: LK.warning }}>Regular</span>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
      <SignalBars level={1} color={LK.error} />
      <span style={{ font: `600 10px/1 ${LK.font}`, color: LK.error }}>Fraco</span>
    </div>
  </div>
);

export const BigVariant = () => (
  <div style={{ display: 'flex', gap: 24, padding: 20, alignItems: 'flex-end', fontFamily: LK.font }}>
    <SignalBars level={4} color={LK.success} big />
    <SignalBars level={3} color={LK.success} big />
    <SignalBars level={2} color={LK.warning} big />
    <SignalBars level={1} color={LK.error} big />
  </div>
);
