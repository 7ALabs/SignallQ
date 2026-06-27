import React from 'react';
import { Overline, LK } from '@signallq/design-system';

export const Default = () => (
  <div style={{ padding: 20, background: LK.bgPrimary, fontFamily: LK.font }}>
    <Overline>Última medição</Overline>
    <div style={{ font: `700 26px/1.2 ${LK.font}`, color: LK.success, marginTop: 8 }}>486 Mbps</div>
  </div>
);

export const Contexts = () => (
  <div style={{ padding: 20, background: LK.bgPrimary, display: 'flex', flexDirection: 'column', gap: 24, fontFamily: LK.font }}>
    <div>
      <Overline style={{ marginBottom: 8 }}>Sua conexão</Overline>
      <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Luiz-5G · Conectado</div>
    </div>
    <div>
      <Overline style={{ marginBottom: 8 }}>Medições recentes</Overline>
      <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Hoje, 18:17 · 486 Mbps</div>
    </div>
    <div>
      <Overline style={{ marginBottom: 8 }}>Experiência de uso</Overline>
      <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Streaming 4K · Ótimo</div>
    </div>
  </div>
);
