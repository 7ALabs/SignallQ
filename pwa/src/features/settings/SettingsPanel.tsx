import { AppShell, Button, Icon, SettingsMenuItem, TopAppBar } from '@/design-system';
import type { ThemePreference } from '@/shared/storage/preferencesRepository';

interface SettingsPanelProps {
  historyCount: number;
  onBack: () => void;
  onClearHistory: () => void;
  onOpenAbout: () => void;
  setThemeMode: (mode: ThemePreference) => void;
  themeMode: ThemePreference;
}

const NAV_ITEMS = [
  { href: '#/home', label: 'Início' },
  { href: '#/historico', label: 'Histórico' },
  { href: '#/ajustes', label: 'Ajustes' },
  { href: '#/sobre', label: 'Sobre' },
];

export function SettingsPanel({ historyCount, onBack, onClearHistory, onOpenAbout, setThemeMode, themeMode }: SettingsPanelProps) {
  return (
    <AppShell
      header={<TopAppBar activeHref="#/ajustes" mobileMode="back" mobileTitle="Ajustes" navItems={NAV_ITEMS} onMobileBack={onBack} />}
      maxWidth={680}
    >
      <div className="sq-settings-screen">
      <div className="sq-settings-section">
        <span className="overline">Aparência</span>
        <div className="sq-settings-card sq-settings-card--row">
          <div className="sq-settings-card__leading">
            <span className="sq-settings-card__icon sq-settings-card__icon--accent">
              <Icon name="dark_mode" size={23} />
            </span>
            <div>
              <strong>Tema</strong>
              <p className="body-small">Escolha como o SignallQ aparece neste navegador.</p>
            </div>
          </div>
          <div className="sq-segmented-control">
            <button
              className={themeMode === 'light' ? 'sq-segmented-control__option sq-segmented-control__option--active' : 'sq-segmented-control__option'}
              onClick={() => setThemeMode('light')}
              type="button"
            >
              <Icon name="light_mode" size={16} />
              Claro
            </button>
            <button
              className={themeMode === 'dark' ? 'sq-segmented-control__option sq-segmented-control__option--active' : 'sq-segmented-control__option'}
              onClick={() => setThemeMode('dark')}
              type="button"
            >
              <Icon name="dark_mode" size={16} />
              Escuro
            </button>
          </div>
        </div>
      </div>

      <div className="sq-settings-section">
        <span className="overline">Dados</span>
        <div className="sq-settings-card sq-settings-card--row">
          <div className="sq-settings-card__leading">
            <span className="sq-settings-card__icon sq-settings-card__icon--error">
              <Icon name="delete_sweep" size={23} />
            </span>
            <div>
              <strong>Limpar histórico</strong>
              <p className="body-small">Remove os {historyCount} testes salvos neste navegador. Pede confirmação.</p>
            </div>
          </div>
          <Button disabled={historyCount === 0} onClick={onClearHistory} variant="danger-outline">
            Limpar
          </Button>
        </div>
      </div>

      <div className="sq-settings-section">
        <span className="overline">Sobre &amp; privacidade</span>
        <div className="sq-settings-card">
          <SettingsMenuItem iconName="shield" label="Privacidade" onClick={onOpenAbout} />
          <SettingsMenuItem iconName="info" label="Sobre o SignallQ" onClick={onOpenAbout} />
          <SettingsMenuItem
            iconColor="tertiary"
            iconName="sell"
            label="Versão do app"
            showChevron={false}
            trailing={<span className="body-small">1.0.0 · web</span>}
          />
        </div>
      </div>
      </div>
    </AppShell>
  );
}
