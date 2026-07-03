import { ReactNode } from 'react';
import { Icon } from '../Icon';

export interface SettingsMenuItemProps {
  iconColor?: 'accent' | 'error' | 'tertiary';
  iconName: string;
  label: string;
  onClick?: () => void;
  showChevron?: boolean;
  trailing?: ReactNode;
}

export function SettingsMenuItem({ iconColor = 'accent', iconName, label, onClick, showChevron = true, trailing }: SettingsMenuItemProps) {
  const content = (
    <>
      <span className="sq-settings-menu-item__leading">
        <span className={`sq-settings-menu-item__icon sq-settings-menu-item__icon--${iconColor}`}>
          <Icon name={iconName} size={22} />
        </span>
        <span className="sq-settings-menu-item__label">{label}</span>
      </span>
      {trailing ?? (showChevron ? <Icon name="chevron_right" size={21} /> : null)}
    </>
  );

  if (onClick) {
    return (
      <button className="sq-settings-menu-item" onClick={onClick} type="button">
        {content}
      </button>
    );
  }

  return <div className="sq-settings-menu-item">{content}</div>;
}
