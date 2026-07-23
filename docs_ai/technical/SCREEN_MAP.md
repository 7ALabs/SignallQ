# Screen Map — Android SignallQ

**Status:** ativo
**Última validação:** 2026-07-23 (contra `AppShell.kt`)
**Fonte de verdade:** código real (`android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/AppShell.kt`)
**Escopo:** navegação do app consumer (tab bar + overlays)
**Responsável:** Lia (Frontend & Design)

> Versão anterior deste documento descrevia a tab bar com `Ajustes` como 5ª aba — isso mudou em
> GH#930 (Fase 1 do plano MD3 To-Be, arquivado em `docs_ai/_archive/2026-07-23_TOBE_MD3_APP_PLANO_IMPLEMENTACAO.md`).
> A tab bar atual usa `Ferramentas`; Ajustes virou overlay `Perfil`, acessado pelo avatar no TopBar.

Todas as telas residem em: `app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/`

---

## NavigationBar — 5 Abas

`tabScreenNames = listOf("home", "speedtest", "sinal_wifi", "historico", "ferramentas")` em
`AppShell.kt`.

| Índice | Label | Composable | Arquivo |
|---|---|---|---|
| 0 | Início | `HomeScreen` | `HomeScreen.kt` |
| 1 | Velocidade | `VelocidadeScreen` (tab) | `VelocidadeScreen.kt` |
| 2 | Sinal | `SinalScreen` | `SinalScreen.kt` |
| 3 | Histórico | `HistoricoScreen` | `HistoricoScreen.kt` |
| 4 | Ferramentas | `FerramentasScreen` | `FerramentasScreen.kt` |

> Não existe aba "Ajustes" nem "Mais". `DispositivosScreen`, diagnóstico de IA e os demais
> itens do hub Ferramentas não são abas — são overlays.
>
> `navigation/AppNavGraph.kt` (se ainda existir) pode conter constantes legadas que não
> correspondem às abas reais — a navegação viva é `selectedTab` (índice 0–4) + `overlayStack`
> (enum `Overlay`, privado) dentro de `AppShell.kt`, não Compose Navigation.

---

## Telas Sobrepostas (Overlays)

`private enum class Overlay` em `AppShell.kt`. Controladas por `overlayStack`, renderizadas via
`AnimatedVisibility` com z-index calculado pela posição na pilha (GH#1098 — corrige bug de
ordem de desenho que não seguia a ordem de empilhamento).

| Overlay | Composable | Arquivo | Trigger | Origem |
|---|---|---|---|---|
| `ResultadoVelocidade` | `ResultadoVelocidadeScreen` | `ResultadoVelocidadeScreen.kt` | Teste de velocidade concluído | Velocidade |
| `Laudo` | `LaudoScreen` | `LaudoScreen.kt` | "Gerar Laudo" (Ferramentas / diagnóstico) | Ferramentas, atalhos Home |
| `Dispositivos` | `DispositivosScreen` | `DispositivosScreen.kt` | Atalho Dispositivos | Ferramentas, atalhos Home |
| `EquipamentoInternet` | `EquipamentoInternetScreen` | `EquipamentoInternetScreen.kt` | Atalho Equipamento de Internet (GH#934 — substitui o antigo `FibraScreen`/`FibraModemScreen` Nokia-only) | Ferramentas |
| `Fibra` | (rota legada, ver nota) | `FibraScreen.kt` | — | mantido no enum, superfície real hoje é `EquipamentoInternet` |
| `Ping` | `PingScreen` | — | Atalho Ping | Ferramentas |
| `Dns` | `DnsScreen`/conteúdo equivalente | — | Atalho DNS (GH#933 — saiu de `ModalBottomSheet` pra tela cheia roteada) | Ferramentas |
| `Jogos` | tela de Jogos | — | Atalho Jogos (GH#935 — catálogo real, ver `docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md`) | Ferramentas |
| `SinalWifi` | indicador dinâmico RSSI/PHY | — | Atalho Sinal WiFi (GH#1201) | Ferramentas |
| `Perfil` | `AjustesScreen` (reorganizado 6a-6f) | `AjustesScreen.kt` | Avatar no TopBar (GH#936 — Fase 7) | TopBar, qualquer tab |
| `Privacidade` | `PrivacidadeScreen` | `PrivacidadeScreen.kt` | Perfil → Privacidade | Perfil |
| `Novidades` | `NovidadesScreen` | `NovidadesScreen.kt` | Perfil → Novidades | Perfil |

**Telas de IA** (`SignallQScreen`/`SignallQPulseScreen`/`LLMChatScreen`, citadas em versão
anterior deste documento) — `[a confirmar]`: não foram encontradas referências no `Overlay`
enum atual de `AppShell.kt`. Podem ter sido removidas (decisão #2 do plano MD3 arquivado previa
isso) ou substituídas por telas dentro do fluxo de diagnóstico (`DiagnosticoScreen`/
`ChatDiagnosticoIaScreen`, ver `docs_ai/technical/AI_FLOW.md`). Confirmar antes de reintroduzir
qualquer rota para essas telas em documentação nova.

---

## Onboarding

| Composable | Arquivo | Acesso |
|---|---|---|
| `OnboardingScreen` | `OnboardingScreen.kt` | Apenas primeira execução (`onboardingConcluidoFlow` no DataStore) |

---

## Arquivos de Suporte à Navegação

| Arquivo | Papel |
|---|---|
| `AppShell.kt` | Shell do app — `NavigationBar` de 5 abas (índice 0–4) + pilha de overlays (`overlayStack`, enum `Overlay`) |
| `MainViewModel.kt` | ViewModel raiz `@HiltViewModel` — expõe os snapshots/estados consumidos pelas telas (2191 linhas — dívida técnica registrada em `.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 4.2) |

---

## Diagrama de Navegação

```
OnboardingScreen (primeira execução)
    ↓
AppShell  (NavigationBar índice 0–4 + overlays)
├── [0] HomeScreen
│       ├── → Dispositivos (overlay)
│       └── → Laudo (overlay)
├── [1] VelocidadeScreen
│       └── → ResultadoVelocidade (overlay)
├── [2] SinalScreen
├── [3] HistoricoScreen
└── [4] FerramentasScreen (hub de atalhos)
        ├── → Dispositivos
        ├── → EquipamentoInternet
        ├── → Ping
        ├── → Dns
        ├── → Laudo
        ├── → Jogos
        └── → SinalWifi

TopBar (qualquer tab) → avatar → Perfil (overlay)
        ├── → Privacidade
        └── → Novidades
```
