import React from 'react';
import { Chip } from '@signallq/design-system';

export function Chips() {
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      <Chip active>Todos</Chip>
      <Chip>Wi-Fi</Chip>
      <Chip>Celular</Chip>
      <Chip disabled>6 GHz</Chip>
    </div>
  );
}
