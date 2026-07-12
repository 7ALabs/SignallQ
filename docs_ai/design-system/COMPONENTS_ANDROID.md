# Componentes Android — SignallQ

**Versão:** v0.23.0 | **Localização:** `app/src/main/kotlin/io/veloo/app/kotlin/ui/component/`  
**Última atualização:** 2026-07-12

25+ componentes custom agrupados por domínio.

> Nota de rebranding (v0.15.0): componentes `Orbit*` foram renomeados para `SignallQ*`. Qualquer referência a `OrbitTopBar`, `OrbitInputArea`, etc. deve ser lida como `SignallQTopBar`, `SignallQInputArea`, etc.

---

## SignallQ — Chat IA (10)

Componentes exclusivos da superfície de IA. Mantêm paleta escura (`#0D0D1A`/`#1A0B2E`/`#1E1130`) independente do tema do app.

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `SignallQTopBar` | `SignallQTopBar.kt` | Header tela IA, título + ações (ex-OrbitTopBar) |
| `SignallQInputArea` | `SignallQInputArea.kt` | Campo entrada texto + botão enviar (ex-OrbitInputArea) |
| `SignallQUserMessageBubble` | `SignallQUserMessageBubble.kt` | Bolha mensagem usuário (ex-OrbitUserMessageBubble) |
| `SignallQAiMessageBubble` | `SignallQAiMessageBubble.kt` | Bolha mensagem IA em markdown (ex-OrbitAiMessageBubble) |
| `SignallQThinkingBubble` | `SignallQThinkingBubble.kt` | Indicador "IA pensando" (ex-OrbitThinkingBubble) |
| `SignallQWelcomeState` | `SignallQWelcomeState.kt` | Tela inicial vazia + prompt (ex-OrbitWelcomeState) |
| `SignallQInlineQuestion` | `SignallQInlineQuestion.kt` | Pergunta inline, resposta rápida (ex-OrbitInlineQuestion) |
| `SignallQActionsCard` | `SignallQActionsCard.kt` | Botões ação sugeridos (ex-OrbitActionsCard) |
| `SignallQTechnicalResultBubble` | `SignallQTechnicalResultBubble.kt` | Card resultados técnicos dentro do chat |
| `SignallQIaHeader` | `SignallQIaHeader.kt` | Header padrão telas IA |

---

## SpeedTest & Pulse (6)

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `GaugeCircular` | `GaugeCircular.kt` | Gauge circular animado (velocidade) |
| `MiniGrafico` | `MiniGrafico.kt` | Gráfico pequeno histórico/mini-trend |
| `PulseResultCard` | `PulseResultCard.kt` | Card resultado monitoramento passivo |
| `SilentSpeedtestIndicator` | `SilentSpeedtestIndicator.kt` | Indicador speedtest background |
| `SignallQSymbol` | `SignallQSymbol.kt` | Logo/marca SignallQ Pulse |
| `SignallQPulseIcon` | `SignallQPulseIcon.kt` | Ícone monitor passivo |

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
| `AiModelFooter` | `AiModelFooter.kt` | Footer com info do modelo IA (nome + versão) |
| `DiagnosisChipsRow` | `DiagnosisChipsRow.kt` | Row de chips de diagnóstico (tags) |
| `ContextualQuestionCard` | `ContextualQuestionCard.kt` | Card de pergunta contextual |
| `SheetDragHandle` | `SheetDragHandle.kt` | Handle drag de bottom sheet |
| `WifiChannelGuide` | `WifiChannelGuide.kt` | Guia visual de canais Wi-Fi |
| `OnDevicePill` | `OnDevicePill.kt` | Indicador "processado no dispositivo" |

---

## Monetização — Anúncio Nativo (8)

`ui/component/ads/` — issue #555 (v0.23.0). Padrão oficial para anúncio nativo (AdMob) dentro
das superfícies orgânicas do app. Nunca placeholder/caixa vazia: todo composable é omitido por
completo (`if (nativeAd == null) return`) quando não há criativo carregado — o layout ao redor
recompõe sem buraco.

| Componente | Arquivo | Propósito |
| --- | --- | --- |
| `NativeAdCard` | `NativeAdCard.kt` | Card cheio, dispensável (X) — Resultado do teste e Histórico |
| `NativeAdRow` | `NativeAdRow.kt` | Linha compacta, não dispensável — Velocidade (estado idle), abaixo de "Último resultado" |
| `NativeAdListRow` | `NativeAdListRow.kt` | Linha dentro da própria lista de dispositivos conectados — Dispositivos |
| `NativeAdCtaButton` | `NativeAdCtaButton.kt` | CTA outline (nunca sólido) — registrado como `callToActionView`, sem `onClick` próprio |
| `NativeAdIconChip` | `NativeAdIconChip.kt` | Chip quadrado com o ícone real do anunciante (`NativeAd.icon`) ou ícone genérico neutro se o criativo não trouxer um |
| `AdBadge` | `AdBadge.kt` | Disclosure obrigatório — "Patrocinado" (AdMob) ou "Parceiro" (afiliado/parceiro) |
| `DashedBorder` | `DashedBorder.kt` | `Modifier.dashedBorder()` — diferenciador visual do Card orgânico, sem equivalente nativo no Compose |
| `NativeAdSource` | `NativeAdSource.kt` | Enum `ADMOB` / `PARTNER` — controla o texto e a cor do `AdBadge` |

**Regras de não-confusão com conteúdo orgânico** (nunca deixar um anúncio passar por card
comum do app):
- Borda **tracejada** (`dashedBorder`), nunca sólida como os cards orgânicos.
- CTA em **outline violeta** (`accent @35%`), nunca sólido — violeta sólido é reservado a ação
  primária orgânica (Iniciar teste, Conversar com IA).
- **Sem foto/hero** — só o ícone do anunciante em chip quadrado (raio ~27% do tamanho, nunca
  círculo — os avatares/ícones orgânicos do app são redondos).
- `AdBadge` sempre visível, nunca escondido atrás de tap/expand.
- Headline/body/CTA sempre vêm do `NativeAd` carregado (criativo real do AdMob) — nunca texto
  hardcoded, isso viola a política de anúncio nativo do próprio AdMob.

**Disclosure — por que dois rótulos.** `AdBadge` distingue duas origens comerciais distintas,
mesmo que hoje só uma esteja em uso:
- **"Patrocinado"** (tom neutro `textTertiary`, ícone `Campaign`) = `NativeAdSource.ADMOB`,
  `native_ad_fallback` puro — sem relação comercial além do ad network. É a única fonte ativa
  nesta entrega, presente nas 4 telas.
- **"Parceiro"** (tom `accentBlue`, ícone `Storefront`) = `NativeAdSource.PARTNER`,
  reservado para `affiliate_product`/`partner_offer` do `coreRecommendation` quando o achado do
  diagnóstico casar com uma oferta curada. Os componentes já suportam essa variante — o catálogo
  de parceiros reais ainda não existe, é trabalho futuro fora do escopo da #555.

**Onde já foi ancorado (referência, não crie posição nova sem revisão da Lia):**

| Tela | Componente | Comportamento |
| --- | --- | --- |
| Velocidade (`SpeedTestScreen.kt`) | `NativeAdRow` | Estado idle, abaixo do card "Último resultado" |
| Resultado (`ResultadoVelocidadeScreen.kt`) | `NativeAdCard` | Dispensável, dismiss persiste só na sessão da tela |
| Dispositivos (`DispositivosScreen.kt`) | `NativeAdListRow` | Dentro da própria lista de dispositivos conectados — **nunca** na seção Infraestrutura (decisão do Luiz, 2026-07-12) |
| Histórico (`HistoricoScreen.kt`) | `NativeAdCard` | Dispensável, mesma regra do Resultado |

---

## Total: 33+ componentes (v0.23.0)

- **Reutilizáveis:** Todos suportam slots e customização via Composable lambdas.
- **Tipados:** TypeScript-like - tudo com @Composable, Modifier, etc.
- **Testáveis:** Sem efeitos colaterais; prévia via @Preview.

---

## Convenção de Nomenclatura

- **SignallQ***: Componentes exclusivos da superfície de IA (chat diagnóstico). Ex.: `SignallQTopBar`, `SignallQInputArea`. Renomeados de `Orbit*` no rebranding v0.15.0.
- **Pulse / SignallQ Pulse**: Componentes do monitoramento passivo.
- **Diag***: Componentes do Diagnóstico IA v0.14.0+ (ex.: `DiagVerdictHeroCard`, `DiagRootCauseCard`, `DiagActionFooter`).
- Sem prefixo genérico (Lk-, App-) — nome direto descreve propósito.

---

## Relacionamentos

```
SignallQScreen usa: SignallQTopBar, SignallQInputArea, SignallQUserMessageBubble,
                   SignallQAiMessageBubble, SignallQWelcomeState, SignallQThinkingBubble,
                   SignallQInlineQuestion, AiModelFooter, AppBorderGlowEffect

LLMChatScreen usa: LLMAssistantMessage, AiModelFooter, (componentes de bolha LLM)

DiagnosticoScreen usa: DiagVerdictHeroCard, DiagRootCauseCard, DiagImpactCard,
                       DiagMetricsGrid, DiagRecommendationCard, DiagActionFooter

SpeedTestScreen usa: GaugeCircular, MiniGrafico, DiagnosisChipsRow

SignallQPulseScreen usa: PulseResultCard, SignallQSymbol, MiniGrafico
```
