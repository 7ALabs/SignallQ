import React from 'react';
import { Icon, LK } from '@signallq/design-system';

export const Outlined = () => (
  <div style={{ display: 'flex', gap: 20, padding: 16, flexWrap: 'wrap', alignItems: 'center', fontFamily: LK.font }}>
    <Icon name="home" size={24} color={LK.textPrimary} />
    <Icon name="wifi" size={24} color={LK.success} />
    <Icon name="speed" size={24} color={LK.accent} />
    <Icon name="auto_awesome" size={24} color={LK.accent} />
    <Icon name="signal_cellular_alt" size={24} color={LK.textSecondary} />
    <Icon name="dns" size={24} color={LK.textSecondary} />
    <Icon name="history" size={24} color={LK.textSecondary} />
    <Icon name="settings" size={24} color={LK.textSecondary} />
  </div>
);

export const Filled = () => (
  <div style={{ display: 'flex', gap: 20, padding: 16, flexWrap: 'wrap', alignItems: 'center', fontFamily: LK.font }}>
    <Icon name="home" size={24} fill={1} color={LK.accent} />
    <Icon name="wifi" size={24} fill={1} color={LK.success} />
    <Icon name="speed" size={24} fill={1} color={LK.accent} />
    <Icon name="auto_awesome" size={24} fill={1} color={LK.accent} />
    <Icon name="check_circle" size={24} fill={1} color={LK.success} />
    <Icon name="warning" size={24} fill={1} color={LK.warning} />
    <Icon name="error" size={24} fill={1} color={LK.error} />
    <Icon name="info" size={24} fill={1} color={LK.accentBlue} />
  </div>
);

export const Sizes = () => (
  <div style={{ display: 'flex', gap: 16, padding: 16, alignItems: 'flex-end', fontFamily: LK.font }}>
    <Icon name="wifi" size={16} color={LK.accent} />
    <Icon name="wifi" size={20} color={LK.accent} />
    <Icon name="wifi" size={24} color={LK.accent} />
    <Icon name="wifi" size={32} color={LK.accent} />
    <Icon name="wifi" size={48} color={LK.accent} />
  </div>
);
