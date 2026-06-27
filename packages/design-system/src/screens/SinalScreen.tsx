import React, { useState } from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';
import { Icon } from '../primitives/Icon.js';
import { SignalBars } from '../primitives/SignalBars.js';
import { Badge } from '../primitives/Badge.js';
import { Card } from '../layout/Card.js';
import { Overline } from '../layout/Overline.js';

export interface SinalScreenProps {
  style?: React.CSSProperties;
}

/** Sinal tab: Wi-Fi networks list with band filters, channel occupancy chart, and mobile signal. */
export function SinalScreen({ style }: SinalScreenProps) {
  const [tab, setTab] = useState<'wifi' | 'canal' | 'movel'>('wifi');
  const [band, setBand] = useState('Todos');

  const tabs = [
    ['wifi', 'Wi-Fi'],
    ['canal', 'Canal'],
    ['movel', 'Móvel'],
  ] as const;

  const bands = ['Todos', '2.4GHz', '5GHz', '6GHz'];

  const others = [
    { ssid: 'Redes ocultas', sub: '2.4GHz', count: '23', lvl: 4 as const, tone: LK.success },
    { ssid: 'Luiz-2.4G', sub: '2.4GHz', count: '2', lvl: 4 as const, tone: LK.success },
    { ssid: 'Wallace lopes 2G', sub: 'Banda: 2.4GHz  RSSI −51 dBm  Canal 11', lvl: 3 as const, tone: LK.success },
    { ssid: 'Adris 2.4', sub: 'Banda: 2.4GHz  RSSI −57 dBm  Canal 1', lvl: 3 as const, tone: LK.success },
  ];

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary, overflow: 'hidden', ...style }}>
      <div style={{ display: 'flex', borderBottom: `1px solid ${LK.border}`, flex: 'none' }}>
        {tabs.map(([id, lbl]) => {
          const on = id === tab;
          return (
            <button
              key={id}
              onClick={() => setTab(id)}
              style={{
                flex: 1,
                background: 'none',
                border: 0,
                cursor: 'pointer',
                padding: '14px 0',
                font: `${on ? 700 : 500} 15px/1 ${LK.font}`,
                color: on ? LK.accent : LK.textSecondary,
                borderBottom: on ? `2px solid ${LK.accent}` : '2px solid transparent',
                marginBottom: -1,
              }}
            >
              {lbl}
            </button>
          );
        })}
      </div>

      {tab === 'wifi' && (
        <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
          <div style={{ display: 'flex', gap: 9, marginBottom: 18 }}>
            {bands.map((b) => {
              const on = b === band;
              return (
                <button
                  key={b}
                  onClick={() => setBand(b)}
                  style={{
                    flex: 1,
                    border: 0,
                    cursor: 'pointer',
                    padding: '12px 0',
                    borderRadius: 999,
                    font: `${on ? 600 : 500} 13px/1 ${LK.font}`,
                    background: on ? hexA(LK.accent, 0.12) : LK.bgSecondary,
                    color: on ? LK.accent : LK.textSecondary,
                  }}
                >
                  {b}
                </button>
              );
            })}
          </div>

          <Overline style={{ marginBottom: 10 }}>Sua conexão</Overline>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: '50%',
                background: hexA(LK.accent, 0.12),
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flex: 'none',
              }}
            >
              <Icon name="wifi" size={22} color={LK.accent} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ font: `600 17px/1.2 ${LK.font}`, color: LK.textPrimary }}>Luiz-5G</span>
                <Badge color={LK.success}>Conectado</Badge>
              </div>
              <div style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textTertiary }}>2 nós detectados</div>
            </div>
          </div>

          <div
            style={{
              display: 'flex',
              gap: 10,
              alignItems: 'center',
              background: hexA(LK.success, 0.12),
              borderRadius: 14,
              padding: '12px 14px',
              margin: '8px 0',
            }}
          >
            <Icon name="router" size={20} color={LK.accent} />
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                <span style={{ font: `700 14px/1 ${LK.font}`, color: LK.accent }}>Conectado agora</span>
                <Badge color={LK.success} bg={hexA(LK.success, 0.2)}>✓ Conectado</Badge>
              </div>
              <div style={{ font: `400 11px/1.4 ${LK.font}`, color: LK.textSecondary }}>
                5GHz · <span style={{ color: LK.success, fontWeight: 600 }}>Excelente</span>
                <br />
                Banda: 5GHz  RSSI −27 dBm  Canal 36
                <br />
                Wi-Fi 5 (ac) · 433 Mbps
              </div>
            </div>
            <SignalBars level={4} color={LK.success} />
          </div>

          <div style={{ display: 'flex', gap: 10, alignItems: 'center', padding: '8px 14px' }}>
            <Icon name="wifi" size={20} color={LK.textTertiary} />
            <div style={{ flex: 1 }}>
              <div style={{ font: `700 14px/1.2 ${LK.font}`, color: LK.textPrimary }}>
                Nó #1{' '}
                <span style={{ color: LK.textTertiary, fontWeight: 400 }}>· 39:83:3d</span>
              </div>
              <div style={{ font: `400 11px/1.4 ${LK.font}`, color: LK.textSecondary }}>
                5GHz · <span style={{ color: LK.warning, fontWeight: 600 }}>Regular</span>
                <br />
                Banda: 5GHz  RSSI −68 dBm  Canal 36
              </div>
            </div>
            <SignalBars level={2} color={LK.warning} />
          </div>

          <Overline style={{ margin: '18px 0 4px' }}>Outras redes</Overline>
          {others.map((n, i) => (
            <div
              key={i}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '13px 0',
                borderBottom: `1px solid ${LK.border}`,
              }}
            >
              <Icon name="wifi" size={22} color={LK.textTertiary} />
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ font: `500 15px/1.2 ${LK.font}`, color: LK.textPrimary }}>{n.ssid}</span>
                  {n.count && (
                    <span style={{ font: `400 12px/1 ${LK.font}`, color: LK.textTertiary }}>· {n.count}</span>
                  )}
                </div>
                <div style={{ font: `400 11px/1.3 ${LK.font}`, color: LK.textTertiary }}>{n.sub}</div>
              </div>
              <SignalBars level={n.lvl} color={n.tone} />
            </div>
          ))}
        </div>
      )}

      {tab === 'canal' && <ChannelTab />}
      {tab === 'movel' && <MovelTab />}
    </div>
  );
}

function ChannelTab() {
  const ch = [
    { c: 'Canal 36', use: 'Sua rede', pct: 30, tone: LK.success },
    { c: 'Canal 112', use: '2 redes', pct: 55, tone: LK.warning },
    { c: 'Canal 161', use: '4 redes', pct: 85, tone: LK.error },
    { c: 'Canal 44', use: 'Livre', pct: 12, tone: LK.success },
  ];
  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
      <Card
        style={{
          background: hexA(LK.warning, 0.08),
          border: `1px solid ${hexA(LK.warning, 0.3)}`,
          marginBottom: 14,
        }}
      >
        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
          <Icon name="lightbulb" size={20} color={LK.warning} />
          <div style={{ font: `400 13px/1.5 ${LK.font}`, color: LK.textPrimary }}>
            <b>Canal 161 congestionado.</b> Troque seu Wi-Fi 5GHz para o <b>canal 44</b> para uma
            conexão mais estável.
          </div>
        </div>
      </Card>
      <Overline style={{ marginBottom: 12 }}>Ocupação dos canais · 5GHz</Overline>
      {ch.map((x) => (
        <div key={x.c} style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ font: `500 13px/1 ${LK.font}`, color: LK.textPrimary }}>{x.c}</span>
            <span style={{ font: `400 12px/1 ${LK.font}`, color: LK.textSecondary }}>{x.use}</span>
          </div>
          <div style={{ height: 8, borderRadius: 4, background: LK.bgSecondary, overflow: 'hidden' }}>
            <div style={{ width: `${x.pct}%`, height: '100%', background: x.tone, borderRadius: 4 }} />
          </div>
        </div>
      ))}
    </div>
  );
}

function MovelTab() {
  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
      <Card
        style={{
          background: hexA(LK.accent, 0.06),
          border: `1px solid ${hexA(LK.accent, 0.25)}`,
          marginBottom: 14,
        }}
      >
        <Overline>Rede móvel · 5G</Overline>
        <div style={{ font: `700 22px/1.2 ${LK.font}`, color: LK.textPrimary, margin: '6px 0 2px' }}>Claro</div>
        <div style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textSecondary }}>RSRP −95 dBm · 5G NR</div>
      </Card>
      {(
        [
          ['signal_cellular_alt', 'Qualidade do sinal', 'Bom — chamadas e vídeos sem cortes', LK.success, 'Bom'],
          ['cell_tower', 'Tipo de conexão', '5G NR — a tecnologia mais rápida disponível', LK.accent, '5G'],
          ['rocket_launch', 'Experiência esperada', 'Ótima para streaming e jogos', LK.success, 'Ótima'],
        ] as const
      ).map(([ic, t, d, tone, bdg]) => (
        <Card
          key={t}
          style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10, padding: 12 }}
        >
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: hexA(tone, 0.1),
              flex: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Icon name={ic} size={20} color={tone} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ font: `600 12px/1.2 ${LK.font}`, color: LK.textPrimary }}>{t}</div>
            <div style={{ font: `400 11px/1.35 ${LK.font}`, color: LK.textSecondary }}>{d}</div>
          </div>
          <Badge color={tone} style={{ fontWeight: 700 }}>{bdg}</Badge>
        </Card>
      ))}
    </div>
  );
}
