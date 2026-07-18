import React from 'react';
import { IconButton } from '@signallq/design-system';

export function Icones() {
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <IconButton name="arrow_back" />
      <IconButton name="share" />
      <IconButton name="refresh" />
      <IconButton name="more_vert" />
    </div>
  );
}
