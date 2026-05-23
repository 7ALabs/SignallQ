# Componentes Android — Linka

**Versão:** v0.6.3 | **Localização:** `linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/component/`

25 componentes custom agrupados por domínio.

---

## Orbit — Chat IA (10)

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `OrbitSymbol` | `OrbitSymbol.kt` | Logo/ícone marca Orbit |
| `OrbitTopBar` | `OrbitTopBar.kt` | Header tela IA, título + ações |
| `OrbitInputArea` | `OrbitInputArea.kt` | Campo entrada texto + botão enviar |
| `OrbitUserMessageBubble` | `OrbitUserMessageBubble.kt` | Bolha mensagem usuário |
| `OrbitAiMessageBubble` | `OrbitAiMessageBubble.kt` | Bolha mensagem IA |
| `OrbitTechnicalResultBubble` | `OrbitTechnicalResultBubble.kt` | Card resultados técnicos dentro chat |
| `OrbitThinkingBubble` | `OrbitThinkingBubble.kt` | Indicador "IA pensando" |
| `OrbitWelcomeState` | `OrbitWelcomeState.kt` | Tela inicial vazia + prompt |
| `OrbitActionsCard` | `OrbitActionsCard.kt` | Botões ação sugeridos |
| `OrbitInlineQuestion` | `OrbitInlineQuestion.kt` | Pergunta inline, resposta rápida |

---

## SpeedTest & Pulse (6)

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `GaugeCircular` | `GaugeCircular.kt` | Gauge circular animado (velocidade) |
| `MiniGrafico` | `MiniGrafico.kt` | Gráfico pequeno histórico/mini-trend |
| `PulseResultCard` | `PulseResultCard.kt` | Card resultado monitoramento passivo |
| `SilentSpeedtestIndicator` | `SilentSpeedtestIndicator.kt` | Indicador speedtest background |
| `LinkaPulseSymbol` | `LinkaPulseSymbol.kt` | Logo/marca LinkaPulse |
| `LinkaPulseIcon` | `LinkaPulseIcon.kt` | Ícone monitor passivo |

---

## Visual & Animação (3)

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `AppBorderGlowEffect` | `AppBorderGlowEffect.kt` | Efeito glow borda (cards destaque) |
| `TypewriterText` | `TypewriterText.kt` | Texto digita caractere-por-caractere |
| `RotatingMessageText` | `RotatingMessageText.kt` | Texto rotaciona entre mensagens |

---

## Layout & Utilitários (5+)

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `LinkaIaHeader` | `LinkaIaHeader.kt` | Header padrão telas IA |
| `AiModelFooter` | `AiModelFooter.kt` | Footer com info modelo IA |
| `DiagnosisChipsRow` | `DiagnosisChipsRow.kt` | Row chips diagnóstico (tags) |
| `ContextualQuestionCard` | `ContextualQuestionCard.kt` | Card pergunta contextual |
| `SheetDragHandle` | `SheetDragHandle.kt` | Handle drag bottom sheet |
| `WifiChannelGuide` | `WifiChannelGuide.kt` | Guia visual canais Wi-Fi |

---

## Total: 25 componentes

- **Reutilizáveis:** Todos suportam slots e customização via Composable lambdas.
- **Tipados:** TypeScript-like - tudo com @Composable, Modifier, etc.
- **Testáveis:** Sem efeitos colaterais; prévia via @Preview.

---

## Convenção de Nomeclatura

- **Orbit***: Componentes exclusivos chat IA.
- **Pulse**: Componentes monitoramento passivo.
- Sem prefixo genérico (Lk-, App-) — nome direto descreve propósito.

---

## Relacionamentos

```
OrbitScreen usa: OrbitTopBar, OrbitInputArea, OrbitUserMessageBubble, 
                OrbitAiMessageBubble, OrbitWelcomeState, OrbitThinkingBubble

ResultScreen usa: GaugeCircular, DiagnosisChipsRow, AppBorderGlowEffect

LinkaPulseScreen usa: PulseResultCard, LinkaPulseSymbol, MiniGrafico
```
