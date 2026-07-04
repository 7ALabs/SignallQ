import { AboutInfoCard, AppShell, TopAppBar } from '@/design-system';

interface AboutScreenProps {
  onBack: () => void;
}

const NAV_ITEMS = [
  { href: '#/home', label: 'Início' },
  { href: '#/historico', label: 'Histórico' },
  { href: '#/ajustes', label: 'Ajustes' },
  { href: '#/sobre', label: 'Sobre' },
];

export function AboutScreen({ onBack }: AboutScreenProps) {
  return (
    <AppShell
      header={<TopAppBar activeHref="#/sobre" mobileMode="back" mobileTitle="Sobre" navItems={NAV_ITEMS} onMobileBack={onBack} />}
      maxWidth={820}
    >
      <div className="sq-about-screen">
        <p className="body-large sq-about-screen__intro">
          Este teste mede a experiência da sua conexão pelo navegador e explica, em linguagem simples, se ela está boa, lenta ou
          instável.
        </p>
        <div className="sq-about-screen__grid">
          <AboutInfoCard
            body="Velocidade de download e upload, ping e jitter — tudo pelo navegador."
            color="success"
            icon="speed"
            title="O que medimos"
          />
          <AboutInfoCard
            body="Sinal Wi-Fi detalhado, perda de pacote nativa e redes próximas não estão disponíveis na web."
            color="warning"
            icon="block"
            title="O que não medimos"
          />
          <AboutInfoCard
            body="O histórico fica salvo apenas neste navegador. O app funciona sem cadastro e não pede senha."
            color="accent"
            icon="lock"
            title="Seus dados"
          />
          <AboutInfoCard
            body="Podemos enviar métricas estruturadas — sem dados pessoais — para gerar um diagnóstico mais claro."
            color="accent"
            icon="auto_awesome"
            title="Se a IA for usada"
          />
        </div>
      </div>
    </AppShell>
  );
}
