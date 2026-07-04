import { AppShell, Icon, QualityBadge, TopAppBar } from '@/design-system';
import type { QualityLevel } from '@/design-system/types';

export interface HomeScreenLatestResult {
  dateLabel: string;
  downloadLabel: string;
  latencyLabel: string;
  qualityLabel: string;
  qualityLevel: QualityLevel;
  uploadLabel: string;
}

interface HomeScreenProps {
  historyCount: number;
  latest: HomeScreenLatestResult | null;
  onOpenAbout: () => void;
  onOpenHistory: () => void;
  onOpenSettings: () => void;
  onStartTest: () => void;
}

const NAV_ITEMS = [
  { href: '#/home', label: 'Início' },
  { href: '#/historico', label: 'Histórico' },
  { href: '#/ajustes', label: 'Ajustes' },
  { href: '#/sobre', label: 'Sobre' },
];

export function HomeScreen({ historyCount, latest, onOpenAbout, onOpenHistory, onOpenSettings, onStartTest }: HomeScreenProps) {
  return (
    <AppShell
      header={
        <TopAppBar
          activeHref="#/home"
          mobileAction={
            <button aria-label="Ajustes" className="sq-icon-button" onClick={onOpenSettings} type="button">
              <Icon name="tune" size={22} />
            </button>
          }
          mobileMode="brand"
          navItems={NAV_ITEMS}
        />
      }
      maxWidth={600}
    >
      <div className="sq-home-screen">
        <h1 className="sq-visually-hidden">Início</h1>

        <div className="sq-home-screen__server-pill">
          <Icon name="dns" size={16} />
          <span>Medição direta pelo navegador</span>
        </div>

        <button className="sq-home-screen__cta" onClick={onStartTest} type="button">
          <Icon name="play_arrow" size={36} />
          <span>Iniciar</span>
        </button>
        <span className="body-medium sq-home-screen__hint">Sem cadastro · leva menos de 1 minuto</span>

        {latest ? (
          <div className="sq-home-screen__result-card">
            <div className="sq-home-screen__result-header">
              <span className="overline">Último resultado · {latest.dateLabel}</span>
              <QualityBadge label={latest.qualityLabel} level={latest.qualityLevel} />
            </div>
            <div className="sq-home-screen__result-grid">
              <div>
                <span className="sq-home-screen__result-label">DOWNLOAD</span>
                <strong className="sq-home-screen__result-value" style={{ color: 'var(--success)' }}>
                  {latest.downloadLabel}
                </strong>
              </div>
              <div>
                <span className="sq-home-screen__result-label">UPLOAD</span>
                <strong className="sq-home-screen__result-value">{latest.uploadLabel}</strong>
              </div>
              <div>
                <span className="sq-home-screen__result-label">LATÊNCIA</span>
                <strong className="sq-home-screen__result-value">{latest.latencyLabel}</strong>
              </div>
            </div>
          </div>
        ) : (
          <div className="sq-home-screen__empty-card">
            <p className="body-medium">Você ainda não fez nenhum teste neste navegador. Toque em Iniciar para medir sua conexão.</p>
          </div>
        )}

        <button className="sq-home-screen__action-row" onClick={onOpenHistory} type="button">
          <span className="sq-home-screen__action-row-icon">
            <Icon name="history" size={19} />
          </span>
          <span className="sq-home-screen__action-row-text">
            <strong>Histórico</strong>
            <span className="body-small">
              {historyCount} {historyCount === 1 ? 'teste salvo' : 'testes salvos'}
            </span>
          </span>
          <Icon name="chevron_right" size={19} />
        </button>

        <button className="sq-home-screen__action-row" onClick={onOpenAbout} type="button">
          <span className="sq-home-screen__action-row-icon">
            <Icon name="info" size={19} />
          </span>
          <span className="sq-home-screen__action-row-text">
            <strong>Sobre o SignallQ</strong>
            <span className="body-small">O que medimos e como tratamos seus dados</span>
          </span>
          <Icon name="chevron_right" size={19} />
        </button>
      </div>
    </AppShell>
  );
}
