import React from 'react';
import { Dialog, LK } from '@signallq/design-system';

export function Confirmacao() {
  return (
    <div style={{ position: 'relative', width: 340, height: 280, background: LK.surfaceContainerLow, borderRadius: 16, overflow: 'hidden' }}>
      <Dialog
        icon="restart_alt"
        title="Reiniciar equipamento?"
        description="A internet ficará indisponível por alguns minutos enquanto o equipamento reinicia."
        confirmLabel="Reiniciar"
        cancelLabel="Cancelar"
        danger
      />
    </div>
  );
}
