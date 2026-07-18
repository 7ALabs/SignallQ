import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';
import { Icon } from '../primitives/Icon.js';
import { hexA } from '../utils.js';

export type TabId = 'home' | 'speed' | 'sinal' | 'hist' | 'ajustes';

const TABS: { id: TabId; label: string; icon: string }[] = [
  { id: 'home', label: 'Início', icon: 'home' },
  { id: 'speed', label: 'Velocidade', icon: 'speed' },
  { id: 'sinal', label: 'Sinal', icon: 'wifi' },
  { id: 'hist', label: 'Histórico', icon: 'history' },
  { id: 'ajustes', label: 'Ajustes', icon: 'settings' },
];

export interface BottomNavProps {
  /** Currently active tab */
  active?: TabId;
  /** Called when the user taps a tab */
  onChange?: (tab: TabId) => void;
}

/** 5-tab bottom navigation bar with accent highlight and pill indicator. */
export function BottomNav({ active = 'home', onChange }: BottomNavProps) {
  const LK = useTokens();
  return (
    <div
      style={{
        display: 'flex',
        background: LK.bgPrimary,
        borderTop: `1px solid ${LK.border}`,
        padding: '8px 4px 6px',
        flex: 'none',
      }}
    >
      {TABS.map((t) => {
        const on = t.id === active;
        return (
          <button
            key={t.id}
            onClick={() => onChange?.(t.id)}
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 4,
              background: 'none',
              border: 0,
              cursor: 'pointer',
              padding: 0,
              color: on ? LK.accent : LK.textTertiary,
            }}
          >
            <div
              style={{
                padding: '3px 18px',
                borderRadius: 999,
                background: on ? hexA(LK.accent, 0.12) : 'transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Icon name={t.icon} size={24} fill={on ? 1 : 0} />
            </div>
            <span style={{ font: `${on ? 600 : 500} 11px/1 ${LK.font}` }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}
