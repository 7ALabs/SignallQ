import React from 'react';
import { Badge, LK } from '@signallq/design-system';

export const Semantic = () => (
  <div style={{ display: 'flex', gap: 10, padding: 20, flexWrap: 'wrap', alignItems: 'center', fontFamily: LK.font }}>
    <Badge color={LK.success}>Conectado</Badge>
    <Badge color={LK.success}>Excelente</Badge>
    <Badge color={LK.warning}>Regular</Badge>
    <Badge color={LK.error}>Queda detectada</Badge>
    <Badge color={LK.accent}>Em andamento</Badge>
    <Badge color={LK.accentBlue}>5G</Badge>
  </div>
);

export const WithCheck = () => (
  <div style={{ display: 'flex', gap: 10, padding: 20, alignItems: 'center', fontFamily: LK.font }}>
    <Badge color={LK.success}>✓ Conectado</Badge>
    <Badge color={LK.accent}>IA ativa</Badge>
    <Badge color={LK.warning}>Oscilação detectada</Badge>
  </div>
);

export const Verdicts = () => (
  <div style={{ display: 'flex', gap: 10, padding: 20, flexWrap: 'wrap', fontFamily: LK.font }}>
    {['Excelente', 'Bom', 'Regular', 'Fraco', 'Forte'].map((v, i) => {
      const colors = [LK.success, LK.success, LK.warning, LK.error, LK.success];
      return <Badge key={v} color={colors[i]} style={{ fontWeight: 700 }}>{v}</Badge>;
    })}
  </div>
);
