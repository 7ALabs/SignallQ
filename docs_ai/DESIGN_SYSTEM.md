# Design System — SignallQ (Android, consumer)

- **Status:** ativo
- **Última validação:** 2026-07-19
- **Fonte de verdade:** este documento é a fonte de verdade **documental** consolidada; a fonte de
  verdade do *código* é `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`
  (`LkColors`, `LkTokens`, `LkSpacing`, `LkRadius`, `signallQTypography`); a fonte de verdade da
  *não-negociáveis* de produto é `.claude/CLAUDE.md`, seção "Design System"
- **Escopo:** app Android SignallQ consumer (`io.signallq.app`), v0.26.0+. Não cobre SignallQ Pro
  (`/signallq-pro-design`, paleta azul `#0B6CFF`) nem o Console (`SignallQ Admin/DESIGN.md`)
- **Responsável:** Lia (conteúdo/tokens/skill), aplicado por Camilo (implementação Android)
- **Documentos substituídos:** consolida `docs_ai/design-system/COLORS.md`,
  `COMPONENTS_ANDROID.md`, `DESIGN_TOKENS.md`, `MD3_GUIDELINES.md`, `SPACING.md`,
  `TYPOGRAPHY.md` (em `docs_ai/_archive/`); expandido em 2026-07-19 com catálogo de estados
  semânticos, sistema de profundidade (4 níveis), regras de gráficos, conteúdo simulado e
  mapeamento Compose

---

## Índice

1. Princípios
2. Cores e nomenclatura semântica
3. Tipografia
4. Espaçamento e grid
5. Raios, bordas e "quando usar o quê"
6. Profundidade e hierarquia de superfícies (4 níveis)
7. Biblioteca de componentes
8. Estados e variantes
9. Regras de gráficos
10. Conteúdo simulado (anúncio nativo / ofertas)
11. Acessibilidade
12. Responsividade
13. Governança
14. Mapeamento Jetpack Compose
15. Ícones
16. Dark mode
17. Copy e regras de texto
18. Localização dos tokens no código
19. Outras fontes do design system (não duplicar)
20. Histórico de divergências corrigidas

---

## 1. Princípios

- **Material 3** via `MaterialTheme` padrão do Jetpack Compose (`androidx.compose.material3`),
  sem sobrescrita de shapes/componentes MD3 custom.
- **Cor de marca fixa** — `lightColorScheme`/`darkColorScheme` com acento fixo, **sem dynamic
  color** do sistema (não deriva do wallpaper).
- **Flat, elevação tonal** — sem sombra dura isolada; profundidade vem primariamente de tint de
  superfície (`surfaceContainer*`), sombra é reforço discreto (ver seção 6).
- **Profundidade comunica hierarquia e interação, nunca decoração.** Um card não deve parecer
  elevado se não for interativo ou prioritário.
- **Métrica crua sempre acompanhada de veredito humano** — nenhum número solto na UI; sempre
  junto de um veredito (Excelente/Bom/Regular/Fraco/Forte).
- **Copy em PT-BR, com "você"** — sentence case em títulos, UPPERCASE em overlines, sem emoji.
  Separador inline padrão: ponto médio (`·`).
- **Superfície SignallQ (IA) descontinuada no To-Be** — não implementar rota ou componente novo
  para essa superfície (ver seção 16).

---

## 2. Cores e nomenclatura semântica

**Fonte de verdade dos valores:** `SignallQTheme.kt` — objeto `LkColors`. Confirmado em código:
os hex abaixo já estão implementados, não são meta futura.

Os nomes abaixo são **aliases documentais** (`color.grupo.papel`) para os hex já existentes —
não são um novo sistema de tokens, servem para falar do mesmo valor de forma inequívoca em
qualquer artefato (doc, protótipo, review).

### Marca (`color.brand.*`)

| Alias | Token real (`LkColors`) | Valor (claro) | Uso |
| --- | --- | --- | --- |
| `color.brand.primary` | `primary` | `#5B21D6` | CTA primário, seleção, nav ativa, marca |
| `color.brand.secondary` | `secondary` | `#2851B8` (azul **fixo**, não deriva do primary) | Chip móvel, DNS privado, links secundários |

**Regra de combinação:** `primary` e `secondary` nunca competem no mesmo elemento pela mesma
função — `primary` é ação/marca/seleção, `secondary` é informação secundária categorizada (rede
móvel, DNS). Não usar `secondary` como CTA principal, não usar `primary` como badge informativo.

### Status semântico (`color.status.*`)

| Alias | Token real | Valor (claro) | Uso permitido | Uso proibido |
| --- | --- | --- | --- | --- |
| `color.status.success` | `success` | `#146C2E` | Conexão boa, teste OK, veredito Excelente/Bom | Decoração, ícone neutro, destaque sem significado |
| `color.status.warning` | `warning` | `#8A5000` | Alerta moderado, veredito Regular | Erro grave, sucesso |
| `color.status.error` | `error` | `#BA1A1A` | Falha crítica, veredito Fraco/Crítico | Alerta moderado, ênfase visual sem falha real |

**Regra dura:** verde só sucesso, âmbar só atenção, vermelho só erro/falha. Nunca usar essas três
cores como paleta decorativa (ex.: ícone verde só porque "combina") — se não há semântica de
status por trás, usar `onSurfaceVariant`/`outline`.

### Dados de rede (`color.data.*`)

| Alias | Token real | Valor (claro) | Uso |
| --- | --- | --- | --- |
| `color.data.latencia` | `phaseLatencia` | `#2563EB` | Fase de latência do SpeedTest, gráficos relacionados |
| `color.data.download` | `phaseDownload` | `#146C2E` | Fase de download |
| `color.data.upload` | `phaseUpload` | `#8A5000` | Fase de upload |

`phaseDownload`/`phaseUpload` reaproveitam os mesmos hex de `success`/`warning` — isso é
intencional (download bom = verde, upload é secundário/moderado por convenção do produto), mas
não confundir contexto: numa fase de SpeedTest a cor identifica a *fase*, não um veredito.

### Superfície (`color.surface.*`)

| Alias | Token real | Valor (claro) | Papel na hierarquia |
| --- | --- | --- | --- |
| `color.surface.background` | `surface` | `#FFFFFF` | Nível 0 — fundo da tela |
| `color.surface.container` | `surfaceContainer` | `#F3EEFA` | Nível 1 — conteúdo agrupado |
| `color.surface.container-high` | `surfaceContainerHigh` | `#ECE5F5` | Nível 2 — interativo/destacado |
| `color.surface.container-highest` | `surfaceContainerHighest` | `#E6DDF2` | Nível 3 — sobreposto |
| `color.surface.selected` | *(novo, formalizado nesta revisão — ver seção 6)* | `surfaceContainerHigh` + borda `primary`@25–30% | Estado selecionado |

Valores completos (claro/escuro, todos os roles MD3) permanecem na tabela original — ver
apêndice A ao final desta seção.

### Regras de uso

- Preferir `LocalLkTokens.current` e `MaterialTheme.colorScheme`.
- `Color(0x...)` fora do tema só é aceitável quando: for cor de marca de terceiro (ex.: logo de
  operadora), for gráfico técnico com paleta própria justificada, ou houver impossibilidade
  prática de representar a cor via token.
- A antiga superfície dedicada de IA pode ter tokens escuros especiais residuais no código por
  compatibilidade/legado — não usar para telas novas do fluxo principal.

### Apêndice A — tabela completa de hex (claro/escuro)

<details>
<summary>Tema claro</summary>

| Token | Valor |
| --- | --- |
| `primary` | `#5B21D6` |
| `onPrimary` | `#FFFFFF` |
| `primaryContainer` | `#EAE0FF` |
| `onPrimaryContainer` | `#210A5C` |
| `secondary` | `#2851B8` |
| `onSecondary` | `#FFFFFF` |
| `secondaryContainer` | `#DCE6FF` |
| `onSecondaryContainer` | `#001A41` |
| `surface` | `#FFFFFF` |
| `surfaceDim` | `#DED8E1` |
| `surfaceContainerLowest` | `#FFFFFF` |
| `surfaceContainerLow` | `#F8F5FB` |
| `surfaceContainer` | `#F3EEFA` |
| `surfaceContainerHigh` | `#ECE5F5` |
| `surfaceContainerHighest` | `#E6DDF2` |
| `onSurface` | `#1C1B1F` |
| `onSurfaceVariant` | `#49454F` |
| `outline` | `#79747E` |
| `outlineVariant` | `#CAC4D0` |
| `inverseSurface` | `#313033` |
| `inverseOnSurface` | `#F4EFF4` |
| `errorContainer` / `onErrorContainer` | `#FFDAD6` / `#410002` |
| `successContainer` / `onSuccessContainer` | `#B6F2BE` / `#04210D` |
| `warningContainer` / `onWarningContainer` | `#FFDDB3` / `#2B1700` |

</details>

<details>
<summary>Tema escuro</summary>

| Token | Valor |
| --- | --- |
| `primary` | `#D0BCFF` |
| `onPrimary` | `#38137E` |
| `primaryContainer` | `#4F2FA8` |
| `onPrimaryContainer` | `#EADDFF` |
| `secondary` | `#AAC7FF` |
| `onSecondary` | `#002E69` |
| `secondaryContainer` | `#1E427A` |
| `onSecondaryContainer` | `#D9E2FF` |
| `surface` | `#131217` |
| `surfaceDim` | `#131217` |
| `surfaceContainerLowest` | `#0E0D12` |
| `surfaceContainerLow` | `#1D1B20` |
| `surfaceContainer` | `#211F26` |
| `surfaceContainerHigh` | `#2B2930` |
| `surfaceContainerHighest` | `#36343B` |
| `onSurface` | `#E6E0E9` |
| `onSurfaceVariant` | `#CAC4D0` |
| `outline` | `#948F99` |
| `outlineVariant` | `#49454F` |
| `error` / `onError` | `#FFB4AB` / `#690005` |
| `success` / `onSuccess` | `#83DA99` / `#00390F` |
| `warning` / `onWarning` | `#FFB870` / `#4A2900` |

</details>

---

## 3. Tipografia

**Fonte de verdade:** `SignallQTheme.kt` — `signallQTypography`.

Família única: **Google Sans Flex** (arquivos `.ttf` embutidos em
`android/app/src/main/res/font/google_sans_flex_*.ttf`, licença SIL OFL 1.1). Nenhuma nova tela
deve introduzir segunda família tipográfica. Pesos em uso: `400` Normal, `500` Medium, `600`
SemiBold, `700` Bold.

| Token | Tamanho | Line height | Peso | Tracking | Uso | Máx. linhas recomendado |
| --- | --- | --- | --- | --- | --- | --- |
| `displaySmall` | 34 sp | 40 sp | Bold | 0 | Métrica hero (velocidade, resultado principal) | 1 |
| `headlineLarge` | 26 sp | 32 sp | Bold | 0 | Título de tela principal | 1–2 |
| `headlineSmall` | 22 sp | 28 sp | SemiBold | 0 | Título de seção grande | 1–2 |
| `titleLarge` | 20 sp | 26 sp | SemiBold | 0 | Título de card/sheet | 1–2 |
| `titleMedium` | 16 sp | 22 sp | Medium | 0.1 | Título de linha/item | 1 |
| `titleSmall` | 14 sp | 20 sp | Medium | 0.1 | Subtítulo, rótulo de campo | 1 |
| `bodyLarge` | 16 sp | 24 sp | Normal | 0.15 | Corpo de texto principal | sem limite rígido |
| `bodyMedium` | 14 sp | 20 sp | Normal | 0.2 | Corpo de texto secundário | sem limite rígido |
| `bodySmall` | 12 sp | 16 sp | Normal | 0.25 | Legenda, texto de apoio | 2–3 |
| `labelLarge` | 14 sp | 20 sp | Medium | 0.1 | Texto de botão | 1 |
| `labelMedium` | 12 sp | 16 sp | Medium | 0.3 | Badge, chip | 1 |
| `labelSmall` | 11 sp | 16 sp | Medium | 0.4 | Overline (+ UPPERCASE) | 1 |

`displayLarge`/`displayMedium`/`headlineMedium` foram removidos da escala (nenhuma tela usa
estilo maior que `displaySmall`) — não reintroduzir sem validar com a Lia.

### Regras de uso

- Preferir sempre `MaterialTheme.typography.*`.
- Evitar `fontSize = ...sp` e `letterSpacing = ...sp` hardcoded em tela/componente comum.
- Só usar `TextStyle(...)` manual com motivo técnico real (canvas, chart labels, renderização
  custom).
- **Dívida conhecida:** ainda existem telas/componentes com `fontSize`/`letterSpacing`
  hardcoded — tratar como dívida de padronização ao tocar na área, não replicar o padrão (ver
  débito registrado ao final deste documento — "~654 literais `.dp` soltos" cobre o mesmo tipo
  de dívida para espaçamento; tipografia hardcoded é dívida irmã, menor em volume).

---

## 4. Espaçamento e grid

**Fonte de verdade:** `LkSpacing` em `SignallQTheme.kt`. Grid de 8dp, 8 degraus:

| Token | Valor | Uso |
| --- | --- | --- |
| `xs` | 4 dp | ajustes finos, separações mínimas, distância ícone-texto compacta |
| `sm` | 8 dp | gaps simples, paddings pequenos, distância ícone-texto padrão |
| `md` | 12 dp | espaçamento padrão interno, gap entre cards de uma mesma seção |
| `base` | 16 dp | padding horizontal principal de tela, padding interno de card, margem de tela padrão |
| `lg` | 20 dp | separações de bloco e grupos mais densos, gap entre seções médias |
| `xl` | 24 dp | separações claras entre seções, distância acima da bottom nav |
| `xxl` | 32 dp | grandes blocos ou respiros, espaço entre seções distintas |
| `xxxl` | 40 dp | grandes aberturas verticais, CTA de onboarding, rodapés |
| `cardContent` | 16 dp | padding interno de card (alias de `base` para o caso específico) |

### Regras de aplicação

- Margem de tela (padding horizontal do conteúdo principal): `base` (16dp).
- Gap entre seções de uma mesma tela: `xl` (24dp) a `xxl` (32dp), conforme densidade da tela.
- Gap entre cards dentro da mesma seção: `md` (12dp).
- Padding interno de card: `base` (16dp), nunca menor que `md` (12dp).
- Distância ícone→texto: `sm` (8dp) em linha padrão, `xs` (4dp) em contexto compacto (chip,
  badge).
- Distância do conteúdo até a bottom nav (`NavigationBar`): pelo menos `xl` (24dp) de respiro
  antes do último elemento visível, sem contar o próprio padding do sistema.
- Safe areas: sempre respeitar `WindowInsets` do sistema (status bar, gesture nav, cutout) —
  nenhum componente deve ser cortado por barra do sistema (ver seção 12).
- Preferir `LkSpacing` a valores literais (`16.dp`, `24.dp` direto no código). Valor fora da
  escala é exceção técnica, não padrão visual. Medidas como `3.dp`, `5.dp`, `10.dp`, `11.dp`,
  `13.dp`, `14.dp` em layout comum são dívida visual conhecida (ver débito registrado ao final).

---

## 5. Raios, bordas e "quando usar o quê"

**Fonte de verdade:** `LkRadius` em `SignallQTheme.kt`.

| Componente | Token | Valor |
| --- | --- | --- |
| Card | `LkRadius.card` | 16 dp |
| SheetFrame | `LkRadius.sheet` | 28 dp (cantos superiores) |
| Button | `LkRadius.button` | 20 dp |
| Field (input) | `LkRadius.input` | 12 dp |
| Chip / Badge | `LkRadius.pill` | 999 dp |
| Dialog | — | 24 dp |

### Quando usar card vs. borda vs. elevação vs. só diferença de superfície

| Situação | Solução recomendada |
| --- | --- |
| Agrupar conteúdo relacionado, sem ação direta | `surfaceContainer` (nível 1), sem borda, sem sombra |
| Separar container/card do fundo | **nunca borda** — diferença tonal de superfície (nível 1+), ver seção 6 |
| Separar duas regiões adjacentes do mesmo tom (ex.: faixa de tabs e conteúdo abaixo) | hairline `1dp outlineVariant` é aceitável — não é separação de container, é divisor entre elementos sem profundidade própria |
| Elemento interativo/selecionável | `surfaceContainerHigh` (nível 2) + leve elevação — nunca borda+sombra ao mesmo tempo sem justificativa |
| Conteúdo já numa lista simples (linha única, texto+ícone) | **não usar card** — usar `LazyColumn` com divisor (`LkSheetDivider`) ou espaçamento, card aqui é ruído visual |
| Dado é a própria tela (ex.: resultado de SpeedTest) | sem card — o fundo da tela já é a superfície |

**Regra de "quando NÃO usar card":** se o conteúdo é uma única linha (ícone+texto+ação) dentro de
uma lista maior, ou se o card não agrega nada além de "uma caixa ao redor de texto", prefira
espaçamento a um card cheio. Card em excesso é a causa mais comum de poluição visual identificada
em auditorias anteriores.

**Decisão do Luiz (2026-07-19) — borda nunca separa container do fundo.** Borda como recurso
passivo de contêiner ("cara de IA", malacabado) está proibida em qualquer componente do Design
System (consumer). A separação de um card/container em relação ao fundo é **sempre** por
diferença tonal de profundidade (seção 6) — nunca por hairline. Borda continua permitida **só**
quando é parte funcional da forma do próprio componente: campo de texto outlined, botão outlined,
checkbox, switch (estado desligado), controle segmentado (outlined segmented button), indicador de
seleção. Divisor funcional entre duas regiões adjacentes do mesmo tom (ex.: `Tabs` — a faixa de
tabs e o conteúdo abaixo não têm profundidade diferente entre si) também é permitido, mas isso não
é "separação de container do fundo" e não deve ser confundido com o padrão proibido.

Achado corrigido nesta revisão (não é débito, já resolvido): `Card` (React, `packages/design-system/src/layout/Card.tsx`)
usava `background: bgCard` que, no tema claro, valia exatamente o mesmo `#FFFFFF` do fundo da tela
(`bgPrimary`) — a única coisa que separava o card do fundo era a borda de 1px. Corrigido trocando o
fundo para `depthLevel1Tint` (`#F3EEFA` claro / `#211F26` escuro) e removendo a borda. Mesmo padrão
corrigido em `BottomNav.tsx` (trocado `borderTop` por fundo `depthLevel1Tint`).

---

## 6. Profundidade e hierarquia de superfícies (4 níveis)

Sistema formal de profundidade, aplicado de forma equivalente nos três produtos do ecossistema
(consumer, Pro, Console) — só os nomes de token mudam por produto. Aqui documentado com os tokens
do consumer (`LkColors`/`colors_and_type.css`).

### Os 4 níveis

| Nível | Papel | Token de superfície | Sombra/borda | Exemplo real no app |
| --- | --- | --- | --- | --- |
| **0 — Fundo da tela** | Plano base, não compete com o conteúdo | `surface` / `background` | Nenhuma | Fundo de `HomeScreen`, `SinalScreen` |
| **1 — Conteúdo agrupado** | Cards comuns, métricas, listas | `surfaceContainer` | Sem sombra, ou quase imperceptível; **nunca borda** — separação é só pelo tint de superfície | Card de resumo, lista de dispositivos |
| **2 — Conteúdo interativo/destacado** | Selecionado, recomendação prioritária, controles interativos | `surfaceContainerHigh` + `color.surface.selected` (novo — borda `primary`@25–30% quando selecionado) | Contraste tonal maior, pode ter borda de destaque suave e sombra discreta | `RecommendationEngineCard` em destaque, rede Wi-Fi conectada |
| **3 — Sobreposto** | Dialogs, bottom sheets, menus, tooltips | `surfaceContainerHighest` | Sombra ou scrim controlado, contraste suficiente | `LkSheetFrame`, `ConfirmacaoDialog`, `LgpdConsentDialog` |

`color.surface.selected` é o token que estava faltando — hoje o estado "selecionado" é resolvido
ad hoc por componente (ex.: cor de texto/ícone muda, mas a superfície nem sempre muda junto).
Formalizado aqui: **seleção = diferença de superfície (`surfaceContainerHigh`) + cor de destaque
(`primary`), nunca só sombra.**

Scrim (nível 3, fundo de dialog/bottom sheet modal): documentado como token oficial, **ainda não
implementado no Kotlin de produção** — hoje só existe no pacote React (`packages/design-system/src/tokens.ts`,
campo `scrim`). Valor alvo: `rgba(0,0,0,.5)` claro / `rgba(0,0,0,.6)` escuro.

### Regras de aplicação (obrigatórias)

- Profundidade comunica hierarquia/interação, **nunca decoração**.
- Card não deve parecer elevado se não for interativo ou prioritário.
- Evitar sombra forte no tema escuro — priorizar elevação tonal (diferença de superfície) sobre
  sombra.
- Sem glow permanente. Sem glassmorphism como linguagem principal (regra já existente,
  reforçada aqui). Sem gradiente em todo componente — gradiente só em: ação principal, estados
  especiais, marca (avatar/logo), promocional secundário, ou visualização de dados quando
  necessário. Hoje usado com moderação (avatar, barra de progresso) — deve continuar assim.
- Nunca misturar borda + sombra + glow + gradiente no mesmo elemento.
- Profundidade igual para componentes equivalentes (dois cards do mesmo tipo têm o mesmo nível,
  em qualquer tela).
- Seleção usa diferença de superfície + cor, não só sombra.
- Cards aninhados no máximo 2 níveis visuais (ex.: card nível 1 pode conter um chip/badge nível
  2, mas não um card nível 1 dentro de outro card nível 1).
- Botão primário pode ter leve elevação, mas sem parecer desconectado do restante da tela.
- Bottom nav é superfície acima do conteúdo, sem sombra pesada e **sem hairline** — a separação é
  só por diferença de tint de superfície (`depthLevel1Tint`/`surfaceContainer`), nunca por borda.
  React (`packages/design-system/src/layout/BottomNav.tsx`) corrigido em 2026-07-19 (removido
  `borderTop`, fundo passou a `depthLevel1Tint`). **Débito identificado, não corrigido nesta
  revisão** (fora do escopo — mexe em `.kt` de produção): o Kotlin (`AppShell.kt`, função que
  monta o `NavigationBar`) já usa `containerColor = c.surfaceContainer` corretamente, mas ainda tem
  `HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)` logo acima da barra — é
  exatamente o padrão de borda-como-separação-de-container que esta decisão elimina. Ajustar
  requer tocar `AppShell.kt` (fora do escopo desta tarefa) — registrar/atualizar issue de higiene
  quando essa mudança for priorizada.
- Bottom sheets/modais com profundidade claramente superior à tela base (nível 3, sempre).

### Tema escuro

- Superfícies mais elevadas ligeiramente mais claras que o fundo (nunca preto absoluto em toda
  camada — confirmado: `surface = #131217`, não `#000000`).
- Contraste tonal progressivo entre os 4 níveis.
- Sombra sutil, complementar — não a forma principal de separação.
- Bordas com baixa opacidade.
- Evitar cards cinza idênticos sobre fundo cinza idêntico (checar visualmente: nível 1 e nível 2
  devem ser discrimináveis sem depender só de zoom).

### Tema claro

- Diferença de branco/cinza-claro entre planos (confirmado: `surface = #FFFFFF`,
  `surfaceContainer = #F3EEFA`, degradê suave). É o tema onde o erro de "card some no fundo" é
  mais fácil de acontecer (diferença tonal menor que no escuro) — checar sempre com o próprio
  `bgCard`/token de fundo, nunca assumir que a borda vai disfarçar uma diferença tonal insuficiente.
- Sombras leves, difusas, pouco opacas.
- Evitar visual de vários cartões flutuando ao mesmo tempo na mesma tela.
- Nunca usar borda para compensar diferença tonal fraca — se o tint não for suficiente, o ajuste é
  no valor do token (`depthLevel1Tint` etc.), não reintroduzir hairline.

### Critérios de aceite

- Usuário percebe claramente fundo / conteúdo / seleção / sobreposição sem precisar de tooltip.
- Card comum não parece modal.
- Modal/bottom sheet se destaca claramente do conteúdo abaixo.
- Elemento interativo é identificável sem depender só de sombra.
- Profundidade é consistente entre todas as telas (mesmo componente, mesmo nível, em qualquer
  lugar do app).
- Tema escuro não pode parecer uma coleção de retângulos cinza no mesmo plano.

**Estado real de implementação (dívida registrada ao final deste documento):** hoje esse sistema
existe **só como documentação/CSS** (`colors_and_type.css`, tokens `--md-sys-elevation-level0-4`)
— zero uso de `.shadow(`/`shadowElevation` em `android/app/`, e só 2 usos isolados de
`tonalElevation` (`AppShell.kt` nav bar em `0.dp`, `LgpdConsentDialog.kt` em `2.dp`), sem relação
com os 4 níveis documentados aqui. Scrim também não existe no Kotlin. Implementação real fica para
tarefa dedicada de Camilo — ver "Débito a registrar" ao final.

---

## 7. Biblioteca de componentes

**Localização real:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/` (33
arquivos + subpasta `ads/`, 8 arquivos).

### TopBar

Dois padrões oficiais:
1. `CenterAlignedTopAppBar` — título centralizado, `ProfileAvatarButton` à esquerda, ação
   contextual à direita. Padrão da maioria das telas de nível superior (Início, Velocidade,
   Sinal, Histórico).
2. TopBar com seta de voltar — telas secundárias/fluxos profundos, título alinhado à esquerda.

Contexto especial (ex.: TopBar de sheet com título+subtítulo) só é aceitável com justificativa
documentada — não criar terceiro padrão sem necessidade real de conteúdo que os dois primeiros
não comportem.

### BottomNav

`NavigationBar` de 5 abas (Início · Velocidade · Sinal · Histórico · Ajustes é overlay, não aba —
ver nota de navegação no `.claude/CLAUDE.md`). Ícone `FILL 1` só no item ativo.

### Botões (variantes)

CTA primário sólido (`primary`), CTA secundário outline, CTA texto/link, botão de ícone
(`IconButton` 40×40 circular), botão destrutivo (usa `error`), botão desabilitado (opacidade
reduzida, sem cor semântica), botão de anúncio nativo (outline violeta, nunca sólido — ver seção
10), segmented button (seletor Rápido/Completo/Triplo do SpeedTest), FAB (quando aplicável).

### Cards (variantes, 10)

Card de resumo/métrica, card de recomendação (`RecommendationEngineCard`), card de rede Wi-Fi
(conectada/disponível), card de dispositivo, card de status de conexão, card informativo
(`LkInfoCallout`), card de anúncio nativo (`NativeAdCard`), card de oferta simulada
(`SimulatedOfferCard`), card de pergunta contextual (`ContextualQuestionCard`), card de resultado
pulsante (`PulseResultCard`). Não misturar padrões entre eles — cada card variante tem seu próprio
arquivo/composable, não reimplementar visualmente um dos 10 sem reaproveitar o componente.

### Chips / Tabs / Segmented / Badges

Chip de filtro (banda Wi-Fi: Todos/2.4/5/6 GHz), chip de status (`LkPillBadge`), badge de
disclosure de anúncio (`AdBadge`), badge de operadora (`OperadoraBadge`), tabs de navegação
interna (ex.: dentro de Fibra/Equipamento). Cada um com raio `pill` (999dp), sem misturar
formato quadrado/pill no mesmo grupo.

### Catálogo de ícones por conceito de rede

Material Symbols Outlined — `wifi` (Wi-Fi), `cell_tower` (rede móvel), `router` (roteador/fibra),
`speed` (velocidade/SpeedTest), `dns` (DNS), `devices` (dispositivos conectados), `history`
(histórico), `signal_cellular_alt` (barras de sinal móvel — glyph vertical customizado via
`SpeedBarsChart`/barras próprias, não o ícone Material puro), `warning`/`error`/`check_circle`
(estados semânticos). Ver seção 15 para regras gerais de ícone.

### Métricas / dados

Padrão obrigatório: **valor + unidade + label + estado + veredito**. Nunca exibir ausência de
dado como se fosse valor válido — usar os símbolos:
- `—` (travessão) quando o dado simplesmente não foi coletado ainda.
- Skeleton/shimmer quando está carregando.
- Estado de erro explícito (ícone + texto) quando a coleta falhou.
- Rótulo "Simulado"/"Estimado" quando o valor não vem de medição real (ver seção 10).

---

## 8. Estados e variantes

### Catálogo de 7 estados semânticos

| Estado | Cor | Ícone | Texto (exemplo) | Uso |
| --- | --- | --- | --- | --- |
| Excelente | `success` | `check_circle` | "Excelente" | Veredito de métrica muito boa |
| Bom | `success` (tom levemente mais neutro se necessário distinguir de Excelente) | `check_circle` | "Bom" | Veredito de métrica boa |
| Regular | `warning` | `warning` | "Regular" | Veredito de métrica mediana |
| Ruim / Fraco | `error` | `error` | "Fraco" | Veredito de métrica ruim |
| Crítico | `error` (ênfase, ex. container) | `error` | "Crítico" | Falha grave, ação urgente necessária |
| Indisponível | `onSurfaceVariant`/`outline` | `block`/`wifi_off` | "Indisponível" | Recurso não aplicável ao contexto (ex. sem SIM) |
| Desconhecido | `onSurfaceVariant`/`outline` | `help` | "Não foi possível medir" | Coleta falhou sem erro classificável |

Regra: cor + ícone + palavra sempre juntos — nunca depender só de cor para comunicar estado
(acessibilidade, seção 11).

### Estados de interação (formalizados)

| Estado | Opacidade/tratamento |
| --- | --- |
| `hover` | 8% overlay sobre `onSurface`/`onPrimary` |
| `focus` | 10% overlay + indicador visível (não só cor) |
| `pressed` | 12% overlay |
| `dragged` | 16% overlay |
| `disabled` | opacidade reduzida (~38%), sem cor semântica aplicada |
| `loading` | skeleton/shimmer no lugar do conteúdo final, nunca card vazio sem indicação |

Vale para: card clicável, itens de lista/sheet, tabs, ações de TopBar, chips tocáveis.

---

## 9. Regras de gráficos

- Legenda sempre visível quando há mais de uma série; nunca depender só de cor pra diferenciar
  série (usar também label/ícone).
- Eixos com unidade explícita (Mbps, ms, dBm, canal).
- Escala e grid discretos — linhas de grid em `outlineVariant`, nunca competindo com o dado.
- Cores seguem `color.data.*` (seção 2) quando o gráfico for de fase de SpeedTest; fora disso,
  usar `primary`/`secondary`/status conforme semântica real.
- Tooltip (quando aplicável) usa nível 3 de profundidade (sobreposto).
- Estado vazio: nunca um gráfico em branco sem explicação — texto "Sem dados suficientes ainda"
  ou equivalente, com o veredito "Desconhecido" (seção 8).

### Regras específicas — gráfico de canais Wi-Fi (`WifiChannelGuide`)

- Canais válidos só por faixa real (2.4 GHz: 1–13 no Brasil; 5 GHz: canais UNII conforme
  regulação; 6 GHz: canais Wi-Fi 6E) — nunca desenhar canal fora da faixa real do padrão.
- Sem sobreposição de labels de canal — se a densidade de redes for alta, agrupar ou truncar
  antes de deixar texto sobreposto.
- Destaque visual claro da rede conectada (nível 2 de profundidade — cor `primary` ou
  `secondary`, conforme a rede seja a ativa do usuário).
- Redes ocultas (SSID vazio/oculto) agrupadas visualmente, não listadas uma a uma competindo com
  redes nomeadas.

---

## 10. Conteúdo simulado (anúncio nativo / ofertas)

Formalizado a partir do que já existe em `SimulatedOfferCard`/`NativeAdCard`/`AdBadge`:

- **Identificação sempre visível** — badge "Patrocinado" (tom neutro, AdMob) ou "Parceiro" (tom
  `secondary`/`accentBlue`, afiliado/parceiro curado). Nunca omitir o disclosure.
- **Hierarquia secundária** — conteúdo patrocinado nunca compete visualmente com o resultado
  orgânico do diagnóstico; usa borda tracejada (`DashedBorder`, nunca sólida), CTA outline
  (nunca sólido — sólido é exclusivo do CTA primário orgânico), sem foto/hero, ícone do
  anunciante em chip quadrado (nunca círculo).
- **Sem promessa técnica não comprovada** — copy de oferta não pode alegar resultado técnico
  específico ("dobra sua velocidade") sem base real.
- Componente inteiro é **omitido** (não vira placeholder vazio) quando não há criativo
  carregado.
- Três variantes por contexto, nunca escolha por preferência: `NativeAdCard` (card cheio,
  dispensável), `NativeAdRow` (linha compacta), `NativeAdListRow` (linha dentro de lista
  existente).

Referência completa: `docs_ai/_archive/COMPONENTS_ANDROID.md` (seção "Monetização — Anúncio
Nativo", preservada no arquivo histórico) e os componentes reais em
`android/app/.../ui/component/ads/`.

---

## 11. Acessibilidade

- **Touch target mínimo: 48dp** — valor MD3/Android padrão. Corrige divergência anterior entre
  `PRODUCT.md` (dizia 56dp) e `.claude/skills/SignallQ-design/HANDOFF_README.md` (dizia 44px) —
  ambos corrigidos nesta consolidação (2026-07-19).
- Contraste de texto conforme MD3 (mínimo AA para texto de corpo, preferencial AAA para texto
  crítico de diagnóstico).
- TalkBack: todo ícone/estado semântico precisa de `contentDescription`/`semantics` equivalente
  ao texto visível — nunca só cor.
- Reduced motion: respeitar preferência do sistema; animações de diagnóstico (loading, pulsante)
  devem ter alternativa estática equivalente.
- Font scaling: layout não pode quebrar com fonte do sistema aumentada — testar até pelo menos
  130% de escala.
- Nunca depender só de cor para status — sempre ícone + cor + palavra (reforça seção 8).

---

## 12. Responsividade

- Tamanhos de tela: telefones Android padrão (compact/medium width classes) — não há suporte
  formal a tablet/foldable ainda; se necessário no futuro, tratar como tarefa dedicada.
- Densidade: layout usa `dp`, nunca `px` fixo.
- Gesture nav vs. button nav: respeitar `WindowInsets` do sistema em ambos os casos — nenhum
  componente cortado pelas barras do sistema.
- Cutouts (notch/câmera): conteúdo crítico nunca atrás de cutout.
- Teclado: campos de entrada (ex. DNS customizado) devem manter o campo visível acima do teclado
  (scroll automático ou `imePadding`).
- Orientação: app é portrait-first; rotação para landscape não quebra layout (mesmo que não seja
  o caso de uso principal).
- **Nenhum componente cortado pelas barras do sistema** — critério de aceite obrigatório em
  qualquer tela nova.

---

## 13. Governança

- Nenhum componente novo sem checar equivalente existente na biblioteca (seção 7).
- Nenhum valor visual hardcoded em tela — sempre token (`LkColors`/`LkSpacing`/`LkRadius`/
  `signallQTypography`).
- Deprecar antes de remover — componente/token antigo ganha nota de depreciação antes de sair do
  código, nunca removido silenciosamente.
- Este documento (`docs_ai/DESIGN_SYSTEM.md`) é a fonte única de verdade documental — protótipo
  (Claude Design) e implementação (Kotlin) usam os mesmos nomes e estados descritos aqui.
- Ao encontrar divergência entre protótipo/documento e código real, registrar a divergência
  explicitamente (não silenciar) e decidir qual lado corrige — nunca presumir que o código
  "deve" seguir o protótipo sem checagem.

---

## 14. Mapeamento Jetpack Compose

Tabela por componente: nome oficial do DS (alias documental), finalidade, variantes, token
usado, implementação real hoje.

| Nome oficial (DS) | Finalidade | Variantes | Token usado | Implementação real |
| --- | --- | --- | --- | --- |
| `SignallQSurfaceCard` | Card base nível 1/2 | preenchido, com borda, selecionado | `surfaceContainer(High)`, `LkRadius.card` | `LkSurfaceCard` (`BaseComponents.kt:35`) |
| `SignallQSectionOverline` | Rótulo de seção UPPERCASE | — | `labelSmall`, `onSurfaceVariant` | `LkSectionOverline` (`BaseComponents.kt:58`) |
| `SignallQPillBadge` | Badge/chip pill | status, neutro | `LkRadius.pill` | `LkPillBadge` (`BaseComponents.kt:73`) |
| `SignallQStatusDot` | Indicador de status pontual | success/warning/error | `color.status.*` | `LkStatusDot` (`BaseComponents.kt:96`) |
| `SignallQSheetSectionTitle` | Título dentro de bottom sheet | — | `titleLarge` | `LkSheetSectionTitle` (`BaseComponents.kt:110`) |
| `SignallQInlineBulletText` | Texto com marcador inline | — | `bodyMedium` | `LkInlineBulletText` (`BaseComponents.kt:133`) |
| `SignallQInfoCallout` | Card informativo (nível 1) | info, aviso | `surfaceContainer`, `color.status.warning` opcional | `LkInfoCallout` (`BaseComponents.kt:161`) |
| `SignallQNumberedStep` | Passo numerado (tutorial/ação) | — | `titleSmall`+`bodyMedium` | `LkNumberedStep` (`BaseComponents.kt:188`) |
| `SignallQSheetInfoRow` | Linha ícone+label+valor dentro de sheet | — | `bodyMedium` | `LkSheetInfoRow` (`BaseComponents.kt:224`) |
| `SignallQSheetDivider` | Divisor dentro de sheet | — | `outlineVariant` | `LkSheetDivider` (`BaseComponents.kt:250`) |
| `SignallQSheetFrame` | Frame de bottom sheet (nível 3) | — | `LkRadius.sheet`, `surfaceContainerHighest` | `LkSheetFrame` (`BaseComponents.kt:256`) |
| `SignallQSymbol` | Ícone Material Symbols | outline, filled | eixo FILL | `LkSymbol` (`LkSymbol.kt:48`) |
| `SignallQConfirmDialog` | Dialog de confirmação (nível 3) | — | raio Dialog 24dp | `ConfirmacaoDialog.kt` |
| `SignallQLgpdDialog` | Dialog de consentimento LGPD | — | `tonalElevation = 2.dp` (único uso real de elevação hoje) | `LgpdConsentDialog.kt` |
| `SignallQGaugeCircular` | Gauge circular de fase (SpeedTest) | latência/download/upload | `color.data.*` | `GaugeCircular.kt` |
| `SignallQMiniGrafico` | Mini gráfico inline | — | `color.data.*`/`outlineVariant` | `MiniGrafico.kt` |
| `SignallQSpeedBarsChart` | Barras de sinal/velocidade | — | `color.status.*` | `SpeedBarsChart.kt` |
| `SignallQWifiChannelGuide` | Gráfico de canais Wi-Fi | 2.4/5/6 GHz | `color.data.*`, `outlineVariant` | `WifiChannelGuide.kt` |
| `SignallQOfflineBanner` | Banner de estado offline | — | `color.status.warning`/`error` | `OfflineBanner.kt` |
| `SignallQOperadoraBadge` | Badge de operadora | — | fundo branco fixo + `outlineVariant` (regra de container de logo, 2026-07-17) | `OperadoraBadge.kt` |
| `SignallQProfileAvatarButton` | Avatar no TopBar (acesso a Ajustes) | — | gradiente `primary→secondary` | `ProfileAvatarButton.kt` |
| `SignallQNativeAdCard` | Card de anúncio nativo | Resultado, Histórico | borda tracejada, CTA outline | `ads/NativeAdCard.kt` |
| `SignallQNativeAdRow` | Linha compacta de anúncio | Velocidade idle | idem | `ads/NativeAdRow.kt` |
| `SignallQNativeAdListRow` | Linha de anúncio dentro de lista | Dispositivos | idem | `ads/NativeAdListRow.kt` |
| `SignallQAdBadge` | Disclosure "Patrocinado"/"Parceiro" | AdMob, afiliado | neutro / `secondary` | `ads/AdBadge.kt` |
| `SignallQSimulatedOfferCard` | Card de oferta simulada/parceiro | — | outline, sem foto | `ads/SimulatedOfferCard.kt` |
| `SignallQRecommendationCard` | Card de recomendação priorizada (nível 2) | destacado | `surfaceContainerHigh` | `RecommendationEngineCard` (referenciado em `ResultadoVelocidadeScreen.kt`, GH#813) |
| `SignallQElevationLevel0-3` | Tokens de profundidade (seção 6) | — | `--md-sys-elevation-level0-4` (CSS) | **não implementado** — só documentação/CSS, ver débito registrado |
| `SignallQScrim` | Scrim de modal/bottom sheet | — | `rgba(0,0,0,.5/.6)` | **não implementado** — só `packages/design-system` (React) |
| `SignallQSurfaceSelected` | Estado de superfície selecionada | — | `surfaceContainerHigh` + borda `primary`@25–30% | **não implementado como token nomeado** — hoje resolvido ad hoc por componente |

**Exemplo correto:**
```kotlin
LkSurfaceCard(modifier = Modifier.padding(LkSpacing.base)) {
    LkSheetSectionTitle(text = "Rede conectada")
    LkSheetInfoRow(label = "Sinal", value = "Forte")
}
```

**Exemplo incorreto (hardcode, evitar):**
```kotlin
Card(
    modifier = Modifier.padding(16.dp), // deveria ser LkSpacing.base
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEFA)) // deveria ser LkColors.surfaceContainer
) { /* ... */ }
```

**Nota de nomenclatura:** os nomes `SignallQ*` acima são **aliases documentais** desta tabela —
o código real ainda usa o prefixo `Lk*` (herdado da marca anterior). Renomear os arquivos reais é
dívida registrada ao final deste documento, não executada nesta consolidação (edição de código de
produção está fora do escopo do trabalho da Lia).

---

## 15. Ícones

Material Symbols (**Outlined**, variable font — `material_symbols_outlined.ttf`, Apache 2.0) —
não-negociável fixado em `.claude/CLAUDE.md` — GH#1008 (fundação). Fonte variável com eixos FILL,
wght, GRAD, opsz; renderiza via ligadura OpenType. Componente `LkSymbol` (GH#1008) encapsula uso,
com suporte a `filled = true` para estado selecionado (eixo FILL) — migração de
`androidx.compose.material.icons.*` tela a tela em andamento.

Não há tabela de mapeamento ícone→uso exaustiva; ao introduzir um ícone novo, usar o catálogo
https://fonts.google.com/icons?icon.set=Material+Symbols e manter peso/densidade consistentes
com o restante da tela. Ver seção 7 para o catálogo de ícones por conceito de rede já em uso.

---

## 16. Dark mode

`SignallQTheme.kt` implementa `lightColorScheme`/`darkColorScheme` fixos com o acento da marca
(seção 2) — **sem dynamic color** do sistema. A paleta escura é completa (todos os roles MD3 com
hex próprios, não apenas inversão automática do claro).

A antiga superfície SignallQ (IA) usava paleta escura fixa própria (`signallQBlack` /
`signallQDarkSurface` / `signallQDarkCard`) que **não seguia** o tema claro/escuro do sistema —
essa superfície está descontinuada no Fluxo de Telas To-Be; não implementar rota ou componente
novo para ela. Tokens residuais podem continuar no código por legado/compatibilidade.

---

## 17. Copy e regras de texto

- PT-BR, com "você" (nunca "tu" ou tratamento formal "o senhor/a senhora").
- Sentence case em títulos (não Title Case, não UPPERCASE).
- UPPERCASE reservado a overlines.
- Sem emoji — decisão de produto, não afetada pelo MD3.
- Separador inline padrão: ponto médio (`·`).
- Métrica crua (Mbps, ms, %) sempre acompanhada de veredito humano: Excelente / Bom / Regular /
  Fraco / Forte (ver catálogo completo de 7 estados na seção 8).

---

## 18. Localização dos tokens no código

Confirmado em código:

- Arquivo: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`
- Objetos: `LkColors` (cores claro/escuro), `LkTokens`, `LkSpacing`, `LkRadius`,
  `signallQTypography`
- Consumo em componentes confirmado via grep de `LkColors` em múltiplos arquivos de
  `ui/component/` (ex.: `BaseComponents.kt`, `ads/AdBadge.kt`, `AnaliseDetalhadaBottomSheet.kt`)

**Nota de caminho físico:** o arquivo mora fisicamente em `io/veloo/app/kotlin/...` embora
declare `package io.signallq.app...` — é a divergência conhecida de ~460 arquivos `.kt`
documentada em `.claude/rules/higiene-e-padronizacao-repositorio.md` (seção 4.1). Não é
específica deste design system; é dívida estrutural do repo inteiro.

---

## 19. Outras fontes do design system (não duplicar aqui)

Existem artefatos paralelos de design no repo, cada um com escopo/finalidade próprios — não são
cópias redundantes entre si mesmo compartilhando os mesmos tokens visuais (referência:
`.claude/CLAUDE.md`, seção "Design System" → "Onde fica cada 'design system'"):

| Onde | Escopo | Finalidade |
| --- | --- | --- |
| `.claude/skills/SignallQ-design/` | Android (app real) | Skill do Claude Code — ativa sozinha ao pedir UI Android; fonte de verdade para gerar código/protótipo on-brand |
| `packages/design-system/` | Android (app real) | Pacote React "fonte do Design System"; sincroniza via `/design-sync` com o projeto [SignallQ Design System](https://claude.ai/design/p/2d25d7a1-31b2-4ac3-881f-72dbc8f35a29) — contagem de componentes: ver projeto |
| `docs_ai/design-system/` (histórico) | Android (app real) | Os seis documentos-fonte consolidados aqui — movidos para `docs_ai/_archive/` |
| `DESIGN.md` / `PRODUCT.md` (raiz do repo) | Android (app real) | Spec no formato da skill `impeccable`, North Star "The Calm Translator" |
| `SignallQ Admin/DESIGN.md` / `PRODUCT.md` | SignallQ Console (Admin) | Mesmo formato impeccable, mas do Console — North Star e paleta próprias, não confundir com o app Android |
| `docs_ai/plataforma/08..11_*` + skill `/signallq-pro-design` | SignallQ Pro | Design do Pro — identidade azul, projeto [SignallQ PRO - Design System](https://claude.ai/design/p/77a19317-ea64-4e47-b55c-578eca776c09), não misturar com este documento |

Não criar artefato de design novo sem checar se já existe em algum destes.

---

## 20. Histórico de divergências corrigidas

**Nesta consolidação (2026-07-19):**
- Touch target: `PRODUCT.md` dizia 56dp, `HANDOFF_README.md` dizia 44px — ambos corrigidos para
  **48dp** (padrão MD3/Android real), ver seção 11.
- `.claude/skills/SignallQ-design/ui_kits/android/chrome.jsx` tinha `accent:'#6C2BFF'` e o
  restante da paleta local (`success`/`warning`/`error`/`bg*`/`text*`/`border`/`rBtn`/`font`)
  travados na era Linka/manual MD3 anterior — corrigido para bater com `colors_and_type.css`
  atual (`#5B21D6`/`#2851B8`, Google Sans Flex, raio de botão 20dp). Como os demais `.jsx` da
  pasta (`app.jsx`, `screens.jsx`, `signallq.jsx`, `speedtest.jsx`) só consomem `LK` de
  `chrome.jsx`, a correção se propaga automaticamente — nenhum outro arquivo tinha hex
  hardcoded duplicado.
- `.claude/CLAUDE.md` tinha contagem contraditória de componentes de `packages/design-system/`
  ("14 + marca Logo" numa tabela, "25 componentes" em outra) — corrigido para 25 (contagem real,
  confirmada pelo texto do próprio `.design-sync/conventions.md`).
- `.claude/CLAUDE.md` tinha o status do SignallQ Pro desatualizado ("ALVO — app não existe") —
  corrigido: `android/pro/` já tem 94 arquivos `.kt` reais (Fase 0/1 mergeadas via PR
  #1159/#1157), mudança de escopo continua exigindo aprovação do Luiz.
- Sistema de profundidade (elevação tonal + scrim) formalizado com 4 níveis nomeados — antes só
  existia como 5 níveis soltos em CSS sem correspondência de código real; agora documentado
  como alvo explícito de implementação (seção 6), com o gap real declarado.

**Histórico anterior (2026-07-16 e 2026-07-13):** ver `docs_ai/design-system/
DECISAO_ALINHAMENTO_TOBE_2026-07-13.md` e `DECISAO_RENOMEACAO_SIGNALLQ_DESIGN_2026-07-11.md` —
preservados como registro, não reescritos.

*Consolidação original gerada a partir de `docs_ai/design-system/{COLORS,COMPONENTS_ANDROID,
DESIGN_TOKENS,MD3_GUIDELINES,SPACING,TYPOGRAPHY}.md`; expandida em 2026-07-19 por delegação da
Claudete (issue de consolidação do DS, ver "Débito a registrar" reportado ao final da entrega).*
