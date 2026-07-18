import React from 'react';
import { Tabs } from '@signallq/design-system';

export function TabsSinal() {
  return (
    <div style={{ width: 320 }}>
      <Tabs options={[['wifi', 'Wi-Fi'], ['canal', 'Canal'], ['movel', 'Móvel']]} value="wifi" />
    </div>
  );
}
