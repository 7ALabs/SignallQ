import React from 'react';
import { ScreenScroll, Card, Overline, LK } from '@signallq/design-system';

export const Default = () => (
  <div style={{ height: 300, display: 'flex', flexDirection: 'column', fontFamily: LK.font, background: LK.bgPrimary }}>
    <ScreenScroll>
      <Card>
        <Overline style={{ marginBottom: 8 }}>Seção 1</Overline>
        <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Conteúdo da primeira seção.</div>
      </Card>
      <Card>
        <Overline style={{ marginBottom: 8 }}>Seção 2</Overline>
        <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Conteúdo da segunda seção.</div>
      </Card>
      <Card>
        <Overline style={{ marginBottom: 8 }}>Seção 3</Overline>
        <div style={{ font: `400 14px/1.5 ${LK.font}`, color: LK.textPrimary }}>Conteúdo da terceira seção.</div>
      </Card>
    </ScreenScroll>
  </div>
);
