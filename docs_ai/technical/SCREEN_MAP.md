---
title: "Screen Map — Android SignallQ"
description: "Mapa de navegação do app consumer (tab bar + overlays) validado contra AppShell.kt."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-16"
---

# Screen Map — Android SignallQ

**Status:** ativo
**Última validação:** 2026-07-23 (contra `AppShell.kt`)
**Fonte de verdade:** código real (`android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShell.kt`)
**Escopo:** navegação do app consumer (tab bar + overlays)
**Responsável:** Lia (Frontend & Design)

> Versão anterior deste documento descrevia a tab bar com `Ajustes` como 5ª aba — isso mudou em
> GH#930 (Fase 1 do plano MD3 To-Be, arquivado em `docs_ai/_archive/2026-07-23_TOBE_MD3_APP_PLANO_IMPLEMENTACAO.md`).
> A barra atual usa `Ferramentas` como quarta raiz; Ajustes virou overlay `Perfil`, acessado pelo avatar no TopBar.

Todas as telas residem em: `app/src/main/kotlin/io/signallq/app/ui/screen/`

---

## NavigationBar — 4 abas

As raízes são `home`, `speedtest`, `historico` e `ferramentas` em `AppShellNavigation.kt`.

| Índice | Label | Composable | Arquivo |
|---|---|---|---|
| 0 | Início | `Inicio2Screen` | `Inicio2Screen.kt` |
| 1 | Velocidade | `SpeedTestScreen` | `SpeedTestScreen.kt` |
| 2 | Histórico | `HistoricoScreen` | `HistoricoScreen.kt` |
| 3 | Ferramentas | `FerramentasScreen` | `FerramentasScreen.kt` |

> Não existe aba "Ajustes" nem "Mais". `DispositivosScreen`, diagnóstico de IA e os demais
> itens do hub Ferramentas não são abas — são overlays.
>
> A navegação viva é `selectedTab` (índice 0–3) + `overlayStack`
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

**Telas de IA** (`SignallQScreen`/`SignallQPulseScreen`/`LLMChatScreen`/`ChatDiagnosticoIaScreen`,
citadas em versão anterior deste documento) — confirmado: nenhuma existe no código nem no
`Overlay` enum de `AppShell.kt`. `SignallQScreen` foi removida na Fase 8 MD3 (GH#937). As demais
nunca chegaram a ter consumidor de UI — eram parte do motor de chat "SignallQ Pulse"
(`SignallQOrchestrator` e as telas `ContextualQuestionCard`/`PulseResultCard`), removido em
GH#1682 por decisão de produto (o app não tem e não terá chat conversacional — #564). O fluxo de
IA real hoje é a "Análise avançada" (`LaudoScreen`, ver `docs_ai/technical/AI_FLOW.md`). Não
reintroduzir rota para nenhuma dessas telas.

---

## Onboarding

| Composable | Arquivo | Acesso |
|---|---|---|
| `OnboardingScreen` | `OnboardingScreen.kt` | Apenas primeira execução (`onboardingConcluidoFlow` no DataStore) |

---

## Arquivos de Suporte à Navegação

| Arquivo | Papel |
|---|---|
| `AppShell.kt` | Shell do app — `NavigationBar` de 4 raízes + pilha de overlays (`overlayStack`, enum `AppShellOverlay`) |
| `MainViewModel.kt` | ViewModel raiz `@HiltViewModel` — expõe os snapshots/estados consumidos pelas telas (2191 linhas — dívida técnica registrada em `.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 4.2) |

---

## Diagrama de Navegação

```
OnboardingScreen (primeira execução)
    ↓
AppShell  (NavigationBar índice 0–3 + overlays)
├── [0] Inicio2Screen
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
