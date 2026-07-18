import React from 'react';
import { Button } from '@signallq/design-system';

export function Variantes() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10, width: 220 }}>
      <Button variant="filled">Medir velocidade</Button>
      <Button variant="tonal">Compartilhar</Button>
      <Button variant="outlined">Testar novamente</Button>
      <Button variant="text" fullWidth={false}>Ir para o início</Button>
      <Button variant="danger" icon="restart_alt">Reiniciar</Button>
    </div>
  );
}
