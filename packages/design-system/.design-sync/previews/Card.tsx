import React from 'react';
import { Card, Overline, Badge, Icon, LK, hexA } from '@signallq/design-system';

export const Default = () => (
  <div style={{ padding: 16, background: LK.bgPrimary, fontFamily: LK.font }}>
    <Card>
      <Overline style={{ marginBottom: 10 }}>Última medição</Overline>
      <div style={{ font: `700 26px/1 ${LK.font}`, color: LK.success }}>
        486 <span style={{ font: `400 13px/1 ${LK.font}`, color: LK.textSecondary }}>Mbps</span>
      </div>
      <div style={{ font: `400 12px/1 ${LK.font}`, color: LK.textTertiary, marginTop: 4 }}>
        há 2 h · Via Wi-Fi
      </div>
    </Card>
  </div>
);

export const StatusCard = () => (
  <div style={{ padding: 16, background: LK.bgPrimary, fontFamily: LK.font }}>
    <Card style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <div
        style={{
          width: 44,
          height: 44,
          borderRadius: '50%',
          background: hexA(LK.success, 0.1),
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Icon name="wifi" size={22} color={LK.success} />
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ font: `600 15px/1.3 ${LK.font}`, color: LK.textPrimary }}>Luiz-5G</div>
        <div style={{ font: `400 11px/1.3 ${LK.font}`, color: LK.textSecondary }}>
          RSSI −27 dBm · Canal 36 · 433 Mbps
        </div>
      </div>
      <Badge color={LK.success}>Forte</Badge>
    </Card>
  </div>
);

export const WarningCard = () => (
  <div style={{ padding: 16, background: LK.bgPrimary, fontFamily: LK.font }}>
    <Card style={{ background: hexA(LK.warning, 0.08), border: `1px solid ${hexA(LK.warning, 0.3)}` }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <Icon name="lightbulb" size={20} color={LK.warning} />
        <div style={{ font: `400 13px/1.5 ${LK.font}`, color: LK.textPrimary }}>
          <b>Canal 161 congestionado.</b> Troque seu Wi-Fi para o canal 44.
        </div>
      </div>
    </Card>
  </div>
);
