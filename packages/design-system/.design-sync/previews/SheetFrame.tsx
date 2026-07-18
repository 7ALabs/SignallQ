import React from 'react';
import { SheetFrame, Overline, LK } from '@signallq/design-system';

export function Sheet() {
  return (
    <div style={{ position: 'relative', width: 340, height: 260, background: LK.surfaceContainerHighest, borderRadius: 16, overflow: 'hidden', display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
      <SheetFrame>
        <Overline>Detalhes da rede</Overline>
        <div style={{ marginTop: 8, font: `400 14px/20px ${LK.font}`, color: LK.onSurfaceVariant }}>
          Conteúdo da bottom sheet — superfície baixa, cantos 28dp e alça no topo.
        </div>
      </SheetFrame>
    </div>
  );
}
