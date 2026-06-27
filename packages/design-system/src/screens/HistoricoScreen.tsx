import React from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';
import { Card } from '../layout/Card.js';
import { Overline } from '../layout/Overline.js';
import { ScreenScroll } from '../layout/ScreenScroll.js';

export interface HistoricoScreenProps {
  style?: React.CSSProperties;
}

/** Histórico tab: uptime stability grid, AI summary, and recent measurements list. */
export function HistoricoScreen({ style }: HistoricoScreenProps) {
  const cells = Array.from({ length: 35 }, (_, i) => {
    const r = (i * 53) % 100;
    return r > 92 ? LK.error : r > 80 ? LK.warning : LK.success;
  });

  return (
    <ScreenScroll>
      <Card>
        <Overline style={{ marginBottom: 12 }}>Estabilidade · últimas 35 medições</Overline>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 6 }}>
          {cells.map((c, i) => (
            <div key={i} style={{ aspectRatio: '1', borderRadius: 4, background: hexA(c, 0.85) }} />
          ))}
        </div>
        <div style={{ display: 'flex', gap: 14, marginTop: 14 }}>
          {(
            [
              ['Estável', LK.success],
              ['Instável', LK.warning],
              ['Queda', LK.error],
            ] as const
          ).map(([l, c]) => (
            <div key={l} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ width: 10, height: 10, borderRadius: 3, background: c, display: 'block' }} />
              <span style={{ font: `400 12px/1 ${LK.font}`, color: LK.textSecondary }}>{l}</span>
            </div>
          ))}
        </div>
      </Card>

      <Card style={{ background: LK.bgSecondary, border: 0 }}>
        <div style={{ font: `400 14px/1.6 ${LK.font}`, color: LK.textPrimary }}>
          Sua internet ficou{' '}
          <b style={{ color: LK.success }}>estável em 89%</b> do tempo nas últimas 24 h. Houve uma
          breve oscilação por volta das 14 h, mas a conexão se recuperou sozinha.
        </div>
      </Card>

      <Overline style={{ marginTop: 4 }}>Medições recentes</Overline>
      {(
        [
          ['Hoje, 18:17', '486 Mbps', LK.success],
          ['Hoje, 14:02', '120 Mbps', LK.warning],
          ['Ontem, 21:40', '502 Mbps', LK.success],
        ] as const
      ).map(([t, v, c]) => (
        <Card
          key={t}
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 14 }}
        >
          <span style={{ font: `400 13px/1 ${LK.font}`, color: LK.textSecondary }}>{t}</span>
          <span style={{ font: `700 15px/1 ${LK.font}`, color: c }}>{v}</span>
        </Card>
      ))}
    </ScreenScroll>
  );
}
