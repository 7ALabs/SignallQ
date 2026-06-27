import React from 'react';
import { LK } from '../tokens.js';
import { Avatar } from '../primitives/Avatar.js';
import { Icon } from '../primitives/Icon.js';

export interface TopBarProps {
  /** Center title text */
  title?: string;
  /** Optional Material Symbols icon shown left of the title */
  icon?: string;
  /** Leading slot — defaults to Avatar */
  leading?: React.ReactNode;
  /** Trailing action slot */
  action?: React.ReactNode;
}

/** CenterAligned top app bar with leading avatar, centered title+icon, and optional action. */
export function TopBar({ title, icon, leading, action }: TopBarProps) {
  return (
    <div
      style={{
        height: 64,
        display: 'flex',
        alignItems: 'center',
        padding: '0 8px 0 16px',
        flex: 'none',
        background: LK.bgPrimary,
      }}
    >
      <div style={{ width: 48, display: 'flex', justifyContent: 'flex-start' }}>
        {leading ?? <Avatar />}
      </div>
      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
        }}
      >
        {icon && <Icon name={icon} size={22} color={LK.textPrimary} />}
        <span style={{ font: `500 18px/1 ${LK.font}`, color: LK.textPrimary }}>{title}</span>
      </div>
      <div style={{ width: 48, display: 'flex', justifyContent: 'flex-end' }}>{action}</div>
    </div>
  );
}
