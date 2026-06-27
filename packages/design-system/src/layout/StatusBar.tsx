import React from 'react';
import { LK } from '../tokens.js';
import { Icon } from '../primitives/Icon.js';

export interface StatusBarProps {
  /** Time string displayed on the left */
  time?: string;
}

/** Android-style status bar with clock, Wi-Fi, 5G, and battery indicators. */
export function StatusBar({ time = '18:28' }: StatusBarProps) {
  return (
    <div
      style={{
        height: 34,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 18px',
        flex: 'none',
        background: LK.bgPrimary,
      }}
    >
      <span style={{ font: `600 15px/1 ${LK.font}`, color: LK.textPrimary }}>{time}</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: LK.textPrimary }}>
        <Icon name="signal_wifi_4_bar" size={17} />
        <span
          style={{
            font: `700 9px/1 ${LK.font}`,
            border: `1.4px solid ${LK.textPrimary}`,
            borderRadius: 3,
            padding: '1px 2px',
          }}
        >
          5G
        </span>
        <Icon name="signal_cellular_alt" size={17} />
        <Icon name="battery_full" size={17} style={{ transform: 'rotate(90deg)' }} />
      </div>
    </div>
  );
}
