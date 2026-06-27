import React from 'react';
import { Avatar, LK } from '@signallq/design-system';

export const Sizes = () => (
  <div style={{ display: 'flex', gap: 16, padding: 20, alignItems: 'flex-end', fontFamily: LK.font }}>
    <Avatar size={32} letter="L" />
    <Avatar size={44} letter="L" />
    <Avatar size={56} letter="L" />
    <Avatar size={72} letter="L" />
  </div>
);

export const Letters = () => (
  <div style={{ display: 'flex', gap: 14, padding: 20, flexWrap: 'wrap', fontFamily: LK.font }}>
    <Avatar size={44} letter="L" />
    <Avatar size={44} letter="M" />
    <Avatar size={44} letter="A" />
    <Avatar size={44} letter="J" />
    <Avatar size={44} letter="R" />
  </div>
);
