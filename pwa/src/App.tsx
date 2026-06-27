import { Activity, BrainCircuit, Clock3, Gauge, History, Settings, Wifi } from 'lucide-react';
import {
  ActionCard,
  AppShell,
  Button,
  ConnectionSummaryCard,
  DiagnosisInsightCard,
  HomeLayout,
  MetricTile,
  NetworkContextCard,
  RecommendationList,
  SpeedHeroCard,
  ThemeProvider,
  TopAppBar,
} from '@/design-system';

const navItems = ['Visão geral', 'Resultados', 'Ajustes'];

export function App() {
  return (
    <ThemeProvider mode="light">
      <AppShell
        header={
          <TopAppBar
            actions={<Button variant="text">Ajuda</Button>}
            navItems={navItems}
            subtitle="M0 Fundação PWA"
            title="SignallQ"
          />
        }
      >
        <HomeLayout
          hero={
            <SpeedHeroCard
              action={<Button icon={<Gauge size={18} />}>Iniciar teste</Button>}
              caption="Base visual para testar velocidade e estabilidade no navegador, sem prometer métricas que a web não mede."
              downloadLabel="Download ainda não medido"
              qualityLabel="Aguardando teste"
              stabilityLabel="Estabilidade será avaliada por amostras HTTP"
              title="Entenda sua conexão em poucos segundos"
              value="--"
            />
          }
          summary={
            <ConnectionSummaryCard
              description="Esta tela valida o Design System oficial da PWA. Os números abaixo são placeholders de interface, não resultado real de medição."
              quality="unknown"
              qualityLabel="Sem diagnóstico ainda"
              title="Pronto para medir quando o fluxo M1 for implementado"
            />
          }
          metrics={
            <>
              <MetricTile
                helperText="Medição HTTP controlada, não ICMP ping."
                icon={<Gauge size={22} />}
                label="Download"
                status="neutral"
                unit="Mbps"
                value="--"
              />
              <MetricTile
                helperText="Depende de endpoint de upload adequado."
                icon={<Activity size={22} />}
                label="Upload"
                status="neutral"
                unit="Mbps"
                value="--"
              />
              <MetricTile
                helperText="Aproximação via fetch/timing do navegador."
                icon={<Clock3 size={22} />}
                label="Latência"
                status="neutral"
                unit="ms"
                value="--"
              />
            </>
          }
          actions={
            <>
              <ActionCard
                description="Ver testes salvos localmente quando a camada de histórico for entregue."
                icon={<History size={22} />}
                meta="Histórico"
                title="Resultados anteriores"
              />
              <ActionCard
                description="Resumo simples e acionável, separado entre velocidade e estabilidade."
                icon={<BrainCircuit size={22} />}
                meta="Diagnóstico"
                title="Análise da conexão"
              />
              <ActionCard
                description="Preferências web sem transformar a PWA em Android encapsulado."
                icon={<Settings size={22} />}
                meta="Ajustes"
                title="Configuração da PWA"
              />
            </>
          }
          insights={
            <>
              <DiagnosisInsightCard
                body="Quando uma métrica não puder ser medida no navegador, a interface deve mostrar essa limitação em vez de preencher valor falso."
                title="Sem métrica inventada"
              />
              <div className="sq-diagnosis-layout">
                <NetworkContextCard
                  items={[
                    { label: 'Tipo de conexão', value: 'Quando disponível' },
                    { label: 'Wi-Fi detalhado', value: 'Indisponível na web' },
                    { label: 'Navegação', value: 'Header e cards' },
                  ]}
                  title="Contexto do navegador"
                />
                <RecommendationList
                  items={[
                    'Comece pelo teste principal antes de abrir detalhes.',
                    'Leia velocidade e estabilidade como sinais separados.',
                    'Use diagnóstico curto, claro e sem jargão desnecessário.',
                  ]}
                  title="Boas práticas da interface"
                />
              </div>
            </>
          }
        />
      </AppShell>
    </ThemeProvider>
  );
}
