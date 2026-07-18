import React from 'react';
import { Logo } from '@signallq/design-system';

export function Simbolo() {
  return <Logo variant="symbol" size={72} />;
}

export function LockupFundoClaro() {
  return (
    <div style={{ background: '#FFFFFF', padding: 24, borderRadius: 12, display: 'inline-block' }}>
      <Logo variant="lockup" background="light" size={40} />
    </div>
  );
}

export function LockupFundoEscuro() {
  return (
    <div style={{ background: '#131217', padding: 24, borderRadius: 12, display: 'inline-block' }}>
      <Logo variant="lockup" background="dark" size={40} />
    </div>
  );
}
