import React from 'react';
import { useTokens } from '../theme/ThemeProvider.js';
import { Icon } from '../primitives/Icon.js';

export interface CheckboxProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
}

/** Checkbox MD3. */
export function Checkbox({ checked = false, onChange }: CheckboxProps) {
  const LK = useTokens();
  return (
    <div
      onClick={() => onChange && onChange(!checked)}
      style={{
        width: 20, height: 20, borderRadius: 4, flex: 'none',
        border: `2px solid ${checked ? LK.primary : LK.outline}`, background: checked ? LK.primary : 'transparent',
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: onChange ? 'pointer' : 'default',
      }}
    >
      {checked && <Icon name="check" size={14} color={LK.onPrimary} />}
    </div>
  );
}
