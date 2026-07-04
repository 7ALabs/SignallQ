import { AppShell, Icon, TopAppBar } from '@/design-system';

interface LandingScreenProps {
  onOpenAbout: () => void;
  onStartTest: () => void;
}

export function LandingScreen({ onOpenAbout, onStartTest }: LandingScreenProps) {
  return (
    <AppShell
      header={
        <TopAppBar
          actions={
            <>
              <a className="sq-landing-link" href="#/sobre">
                Como funciona
              </a>
              <button className="sq-landing-link sq-landing-link--button" onClick={onOpenAbout} type="button">
                Privacidade
              </button>
            </>
          }
          mobileMode="brand"
        />
      }
      maxWidth={760}
    >
      <div className="sq-landing">
        <span className="overline">Diagnóstico de internet com IA</span>
        <h1 className="sq-landing__title">O raio-x completo da sua internet</h1>
        <p className="body-large sq-landing__lead">
          Muito além de um teste de velocidade: medimos download, upload, latência e estabilidade, e a SignallQ traduz tudo em
          um diagnóstico claro — com recomendações para melhorar sua conexão.
        </p>
        <div className="sq-landing__server-pill">
          <Icon name="dns" size={15} />
          <span>Medição direta pelo navegador</span>
        </div>
        <button className="sq-landing__cta" onClick={onStartTest} type="button">
          <Icon name="play_arrow" size={34} />
          <span>Iniciar</span>
        </button>
        <span className="body-medium sq-landing__hint">Sem cadastro · leva menos de 1 minuto</span>
        <div className="sq-landing__benefits">
          <div className="sq-landing__benefit">
            <Icon name="speed" size={17} style={{ color: 'var(--success)' }} />
            Velocidade
          </div>
          <div className="sq-landing__benefit">
            <Icon name="show_chart" size={17} style={{ color: 'var(--accent-blue)' }} />
            Estabilidade
          </div>
          <div className="sq-landing__benefit">
            <Icon name="auto_awesome" size={17} style={{ color: 'var(--accent)' }} />
            Diagnóstico com IA
          </div>
        </div>
      </div>
    </AppShell>
  );
}
