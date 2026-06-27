import React from 'react';
import { LK } from '../tokens.js';
import { hexA } from '../utils.js';
import { Icon } from '../primitives/Icon.js';
import { Card } from '../layout/Card.js';
import { Overline } from '../layout/Overline.js';

export interface AjustesScreenProps {
  style?: React.CSSProperties;
}

/** Ajustes tab: connection settings, appearance, monitoring toggles, and app info. */
export function AjustesScreen({ style }: AjustesScreenProps) {
  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <div style={{ marginBottom: 4 }}>
      <Overline style={{ margin: '8px 4px 8px' }}>{title}</Overline>
      <Card style={{ padding: 0, overflow: 'hidden' }}>{children}</Card>
    </div>
  );

  const Row = ({
    icon,
    label,
    value,
    last,
    toggle,
    on,
  }: {
    icon: string;
    label: string;
    value?: string;
    last?: boolean;
    toggle?: boolean;
    on?: boolean;
  }) => (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        padding: '14px 16px',
        borderBottom: last ? '0' : `1px solid ${LK.border}`,
      }}
    >
      <div
        style={{
          width: 34,
          height: 34,
          borderRadius: '50%',
          background: hexA(LK.accent, 0.12),
          flex: 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Icon name={icon} size={19} color={LK.accent} />
      </div>
      <span style={{ flex: 1, font: `400 15px/1.2 ${LK.font}`, color: LK.textPrimary }}>{label}</span>
      {value && <span style={{ font: `400 13px/1 ${LK.font}`, color: LK.textSecondary }}>{value}</span>}
      {toggle && (
        <div
          style={{
            width: 44,
            height: 26,
            borderRadius: 13,
            background: on ? LK.accent : LK.border,
            position: 'relative',
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: 3,
              left: on ? 21 : 3,
              width: 20,
              height: 20,
              borderRadius: '50%',
              background: '#fff',
              boxShadow: '0 1px 3px rgba(0,0,0,.2)',
            }}
          />
        </div>
      )}
    </div>
  );

  return (
    <div
      style={{
        flex: 1,
        overflowY: 'auto',
        background: LK.bgPrimary,
        padding: '8px 16px 20px',
        ...style,
      }}
    >
      <Section title="Minha conexão">
        <Row icon="business" label="Operadora" value="Claro" />
        <Row icon="speed" label="Plano contratado" value="500 Mbps" />
        <Row icon="location_on" label="Cidade" value="São Paulo, SP" last />
      </Section>
      <Section title="Aparência">
        <Row icon="dark_mode" label="Tema" value="Sistema" last />
      </Section>
      <Section title="Histórico e dados">
        <Row icon="monitoring" label="Monitoramento ativo" toggle on />
        <Row icon="notifications" label="Alertas de latência" toggle on />
        <Row icon="wifi" label="Alerta de sinal fraco" toggle on={false} last />
      </Section>
      <Section title="Informações">
        <Row icon="lock" label="Privacidade" />
        <Row icon="campaign" label="Novidades" />
        <Row icon="info" label="Versão do app" value="0.21.0 (52)" last />
      </Section>
    </div>
  );
}
