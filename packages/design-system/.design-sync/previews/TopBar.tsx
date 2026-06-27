import React from 'react';
import { TopBar, Icon, Avatar, LK } from '@signallq/design-system';

export const Default = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <TopBar title="Início" />
  </div>
);

export const WithAction = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <TopBar
      title="Sinal"
      icon="settings_input_antenna"
      action={
        <button style={{ background: 'none', border: 0, cursor: 'pointer', padding: 8 }}>
          <Icon name="refresh" size={22} color={LK.textPrimary} />
        </button>
      }
    />
  </div>
);

export const Velocidade = () => (
  <div style={{ background: LK.bgPrimary, fontFamily: LK.font }}>
    <TopBar
      title="Velocidade"
      action={
        <button style={{ background: 'none', border: 0, cursor: 'pointer', padding: 8 }}>
          <Icon name="ios_share" size={20} color={LK.textPrimary} />
        </button>
      }
    />
  </div>
);
