import React from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';
import { Icon } from '../primitives/Icon.js';
import { SignalBars } from '../primitives/SignalBars.js';
import { Card } from '../layout/Card.js';
import { Overline } from '../layout/Overline.js';
import { ScreenScroll } from '../layout/ScreenScroll.js';

export interface HomeScreenProps {
  /** Called when the user taps a navigation shortcut */
  onNavigate?: (dest: string) => void;
}

/** Início tab: network path, last measurement card, quick-action row, signal summary. */
export function HomeScreen({ onNavigate }: HomeScreenProps) {
  const node = (icon: string, label: string, tone: string, active: boolean) => (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 7, flex: 1, zIndex: 1 }}>
      <div
        style={{
          width: 52,
          height: 52,
          borderRadius: '50%',
          background: active ? hexA(tone, 0.12) : LK.bgSecondary,
          border: active ? `1.5px solid ${hexA(tone, 0.4)}` : `1px solid ${LK.border}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Icon name={icon} size={24} color={active ? tone : LK.textTertiary} />
      </div>
      <span style={{ font: `600 11px/1.2 ${LK.font}`, color: LK.textPrimary }}>{label}</span>
    </div>
  );

  return (
    <ScreenScroll>
      {/* NetworkPath */}
      <Card>
        <Overline style={{ marginBottom: 14 }}>Caminho da sua internet</Overline>
        <div style={{ position: 'relative', display: 'flex', justifyContent: 'space-between' }}>
          <div
            style={{
              position: 'absolute',
              top: 26,
              left: '18%',
              right: '18%',
              height: 2,
              background: `linear-gradient(90deg, ${LK.success}, ${LK.accent})`,
            }}
          />
          {node('smartphone', 'Seu aparelho', LK.success, true)}
          {node('router', 'Roteador', LK.accent, true)}
          {node('public', 'Provedor', LK.accentBlue, true)}
        </div>
        <div
          style={{
            marginTop: 14,
            font: `400 12px/1.45 ${LK.font}`,
            color: LK.textSecondary,
            textAlign: 'center',
          }}
        >
          Tudo conectado. Sua conexão chega até o provedor sem falhas.
        </div>
      </Card>

      {/* MedicoesCard */}
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <Overline>Última medição</Overline>
          <span style={{ font: `400 11px/1 ${LK.font}`, color: LK.textTertiary }}>há 2 h</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 18, margin: '14px 0 4px' }}>
          <div>
            <div style={{ font: `400 11px/1 ${LK.font}`, color: LK.textTertiary, marginBottom: 4 }}>Download</div>
            <div style={{ font: `700 26px/1 ${LK.font}`, color: LK.success }}>
              486
              <span style={{ font: `400 12px/1 ${LK.font}`, color: LK.textSecondary, marginLeft: 3 }}>Mbps</span>
            </div>
          </div>
          <div>
            <div style={{ font: `400 11px/1 ${LK.font}`, color: LK.textTertiary, marginBottom: 4 }}>Upload</div>
            <div style={{ font: `700 26px/1 ${LK.font}`, color: LK.accent }}>
              212
              <span style={{ font: `400 12px/1 ${LK.font}`, color: LK.textSecondary, marginLeft: 3 }}>Mbps</span>
            </div>
          </div>
          <svg width="92" height="40" viewBox="0 0 92 40" style={{ marginLeft: 'auto' }}>
            <polyline
              points="0,30 14,24 28,28 42,14 56,18 70,8 92,12"
              fill="none"
              stroke={LK.accent}
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <button
          onClick={() => onNavigate?.('speed')}
          style={{
            width: '100%',
            marginTop: 12,
            border: 0,
            cursor: 'pointer',
            background: LK.accent,
            color: '#fff',
            font: `500 14px/1 ${LK.font}`,
            borderRadius: LK.rBtn,
            padding: '14px',
          }}
        >
          Medir velocidade
        </button>
      </Card>

      {/* MiniCardsRow */}
      <div style={{ display: 'flex', gap: 10 }}>
        {(
          [
            ['dns', 'Testar DNS', 'dns'],
            ['network_ping', 'Ping', 'ping'],
            ['auto_awesome', 'Diagnóstico IA', 'signallq'],
          ] as const
        ).map(([ic, lbl, dest]) => (
          <Card
            key={lbl}
            onClick={() => onNavigate?.(dest)}
            style={{ flex: 1, padding: '14px 8px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}
          >
            <Icon name={ic} size={22} color={LK.accent} />
            <span style={{ font: `500 12px/1.2 ${LK.font}`, color: LK.textPrimary, textAlign: 'center' }}>{lbl}</span>
          </Card>
        ))}
      </div>

      {/* SignalCard */}
      <Card style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: '50%',
            flex: 'none',
            background: hexA(LK.success, 0.1),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Icon name="wifi" size={22} color={LK.success} />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <Overline>Wi-Fi · 5 GHz</Overline>
          <div style={{ font: `600 15px/1.3 ${LK.font}`, color: LK.textPrimary, margin: '1px 0' }}>Luiz-5G</div>
          <div style={{ font: `400 11px/1.3 ${LK.font}`, color: LK.textSecondary }}>
            RSSI −27 dBm · Canal 36 · 433 Mbps
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <SignalBars level={4} color={LK.success} />
          <div style={{ font: `600 10px/1 ${LK.font}`, color: LK.success, marginTop: 5 }}>Forte</div>
        </div>
      </Card>
    </ScreenScroll>
  );
}
