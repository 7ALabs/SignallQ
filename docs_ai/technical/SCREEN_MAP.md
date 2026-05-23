# Screen Map — Android Linka

**Última atualização:** 2026-05-17
**Fonte:** código real (Marcelo, 2026-05-17)

Todas as telas residem em:
`app/src/main/kotlin/io/linka/app/kotlin/ui/screen/`

---

## NavigationBar — 5 Abas

| Índice | Label | Composable | Arquivo |
|---|---|---|---|
| 0 | Home | `HomeScreen` | `HomeScreen.kt` |
| 1 | Testes | `SpeedTestScreen` | `SpeedTestScreen.kt` |
| 2 | Sinal | `SinalScreen` | `SinalScreen.kt` |
| 3 | Dispositivos | `DispositivosScreen` | `DispositivosScreen.kt` |
| 4 | Histórico | `HistoricoScreen` | `HistoricoScreen.kt` |

---

## Fluxos Secundários (Sobrepostos)

Controlados por estado booleano no `MainViewModel`. Não são rotas de Navigation Component separadas. Sobrepostos via `AnimatedVisibility` com `slideInVertically`.

| Composable | Arquivo | Trigger | Retorno |
|---|---|---|---|
| `VelocidadeScreen` | `VelocidadeScreen.kt` | Teste iniciado em SpeedTestScreen | Automático → ResultadoVelocidadeScreen |
| `ResultadoVelocidadeScreen` | `ResultadoVelocidadeScreen.kt` | Teste concluído | → ChatScreen, DiagnosticoScreen, HomeScreen |
| `DiagnosticoScreen` | `DiagnosticoScreen.kt` | ExploreToolsRow ou ResultadoVelocidade | → ChatScreen |
| `ChatScreen` | `ChatScreen.kt` | Botão "Conversar com IA" | → ResultadoVelocidadeScreen (voltar) |
| `FibraScreen` | `FibraScreen.kt` | AjustesScreen → seção Fibra | → AjustesScreen (voltar) |
| `LaudoScreen` | `LaudoScreen.kt` | AjustesScreen → "Gerar Laudo" | → AjustesScreen (voltar) |

---

## Drawer / Menu

| Composable | Arquivo | Quando exibido |
|---|---|---|
| `AjustesScreen` | `AjustesScreen.kt` | Via drawer/menu lateral |
| `OnboardingScreen` | `OnboardingScreen.kt` | Apenas primeira execução (`onboarding_concluido = false` no DataStore) |

---

## Telas de Status / Overlay

| Composable | Arquivo | Descrição |
|---|---|---|
| `OrbitScreen` | `OrbitScreen.kt` | Símbolo animado do Orbit |
| `LinkaPulseScreen` | `LinkaPulseScreen.kt` | Dashboard de monitoramento contínuo |

---

## Arquivos de Suporte à Navegação

| Arquivo | Papel |
|---|---|
| `AppShell.kt` | Shell do app — `NavigationBar` de 5 abas + gerencia sobreposição de fluxos secundários |
| `AppNavGraph.kt` | Define as 5 rotas de aba (`home`, `testes`, `sinal`, `dispositivos`, `historico`) |
| `MainViewModel.kt` | ViewModel raiz — estados booleanos controlam quais telas secundárias estão visíveis |

---

## Diagrama de Navegação

```
OnboardingScreen (primeira execução)
    ↓
AppShell
├── [0] HomeScreen
│       └── → SpeedTestScreen
├── [1] SpeedTestScreen
│       ├── → VelocidadeScreen (auto ao iniciar)
│       │       └── → ResultadoVelocidadeScreen (auto ao concluir)
│       │               ├── → ChatScreen (Orbit IA)
│       │               │       └── ← volta para ResultadoVelocidadeScreen
│       │               └── → DiagnosticoScreen
│       └── → DnsBenchmarkBottomSheet
├── [2] SinalScreen
│       └── → TopologiaBottomSheet (inline)
├── [3] DispositivosScreen
├── [4] HistoricoScreen
└── [Menu] AjustesScreen
        ├── → FibraScreen
        └── → LaudoScreen
```
