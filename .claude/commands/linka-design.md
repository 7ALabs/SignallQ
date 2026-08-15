---
description: Guardião do design system do SignallQ — cria, revisa e orienta UI/Compose garantindo consistência visual com os tokens, tipografia, espaçamento e padrões da marca.
argument-hint: [create <NomeDaScreen>|review <arquivo.kt>|tokens]
allowed-tools: Read(*), Bash(*)
---

## Sistema de design atual (lido em tempo real)

**Theme e tokens vivos:**
!`cat "${CLAUDE_PROJECT_DIR:-.}/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt" 2>/dev/null || echo "(SignallQTheme.kt não encontrado no path esperado)"`

**Componentes reutilizáveis disponíveis:**
!`ls "${CLAUDE_PROJECT_DIR:-.}/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/" 2>/dev/null | grep ".kt" || echo "(diretório não encontrado)"`

**Telas existentes (referência de padrão):**
!`ls "${CLAUDE_PROJECT_DIR:-.}/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/" 2>/dev/null | grep ".kt" || echo "(diretório não encontrado)"`

> ⚠️ Path físico `io/veloo/` é dívida conhecida ([higiene §4.1](../rules/higiene-e-padronizacao-repositorio.md)). Package Kotlin declarado é `io.signallq.app.*`.

---

## Regras do design system SignallQ

> **Fonte da verdade visual e detalhada:** [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) e a skill [`SignallQ-design`](../skills/SignallQ-design/). Em caso de divergência com o resumo abaixo, aqueles vencem — são gerados/derivados a partir do `SignallQTheme.kt` real.

### Identidade da marca

- Nome do produto: **SignallQ**.
- Tom: objetivo, confiável, técnico-acessível — sem jargão.
- Logo: usar apenas os assets oficiais em `brand/` (fonte da verdade cross-produto). Nunca recriar com texto/Font.

### Cores — usar SEMPRE os tokens, nunca hex literal

**Cores semânticas (independentes de tema):**

| Token | Valor | Uso |
|---|---|---|
| `LkColors.accent` | `#6C2BFF` | Botões primários, estados ativos, elementos interativos, ícones de destaque |
| `LkColors.accentBlue` | `#2563EB` | Gradiente do avatar de perfil (accent→accentBlue), acentos secundários |
| `LkColors.success` | `#22C55E` | Status positivo, conexão OK, diagnóstico aprovado |
| `LkColors.warning` | `#F5A623` | Alertas, estados de atenção |
| `LkColors.error` | `#FF4D4F` | Erros, ações destrutivas, falha de diagnóstico |

**Cores adaptativas via `LocalLkTokens.current` (mudam com o tema):**

| Token | Uso |
|---|---|
| `tokens.bgPrimary` | Fundo do Scaffold / tela completa |
| `tokens.bgSecondary` | Fundo de cards, seções elevadas, chips desativados |
| `tokens.bgCard` | Superfície de Card |
| `tokens.textPrimary` | Texto principal, títulos, métricas |
| `tokens.textSecondary` | Texto secundário, descrições, labels de apoio |
| `tokens.textTertiary` | Placeholders, hints, texto de baixo contraste |
| `tokens.border` | Divisores, strokes, bordas de input |
| `tokens.warningContainer` / `tokens.onWarningContainer` | Chip/card de aviso (âmbar) |
| `tokens.amberSurface` | Superfície âmbar suave |
| `tokens.successContainer` / `tokens.onSuccessContainer` | Chip/card de sucesso (verde) |

**Paleta exclusiva SignallQ IA (always-dark — não adaptativa):**

| Token | Uso |
|---|---|
| `LkColors.signallQBlack` | Fundo primário das telas de IA |
| `LkColors.signallQDarkSurface` | Gradiente superior, header da tela de IA |
| `LkColors.signallQDarkCard` | Cards dentro das telas de IA |
| `LkColors.signallQTextOnDark` | Texto primário sobre fundos IA |
| `LkColors.signallQTextSecondaryOnDark` | Texto secundário sobre fundos IA |

**Cores de fase do Speedtest:**

| Token | Uso |
|---|---|
| `LkColors.phaseLatencia` | Indicadores de ping/latência |
| `LkColors.phaseDownload` | Indicadores de download |
| `LkColors.phaseUpload` | Indicadores de upload |

**Proibido:** hex literal no código UI (ex.: `Color(0xFF6C2BFF)`). Sempre referenciar pelo token. Existe migração em andamento; código novo usa **apenas** `LkColors` e `LocalLkTokens`.

---

### Tipografia — Material 3 + escala SignallQ

Usar via `MaterialTheme.typography.*` mapeado para a escala do projeto:

| Token Material 3 | Papel SignallQ | Tamanho | Peso | Uso |
|---|---|---|---|---|
| `displayLarge` | `metric` / h1 | 34sp | 700 | Score principal, velocidade DL/UL grande |
| `headlineMedium` | `sectionTitle` / h2 | 20sp | 600 | Título de seção/capítulo |
| `titleMedium` | `cardTitle` / h3 | 15sp | 500 | Título de card |
| `bodyMedium` | `body` | 14sp | 400 | Corpo de texto, descrições |
| `labelSmall` | `label` / `status` | 12sp | 400/600 | Badges, captions, hints |

**Dados técnicos** (IP, DNS, BSSID, MAC, ASN, canal, frequência): usar Geist Mono quando disponível.
**Fonte do produto:** Geist Sans em todos os textos de interface.

---

### Espaçamento — usar SEMPRE `LkSpacing`

```kotlin
LkSpacing.xs  = 4.dp   // separação mínima entre ícone e texto
LkSpacing.sm  = 8.dp   // padding interno de chips, gap entre items pequenos
LkSpacing.md  = 12.dp  // gap entre elementos dentro de um card
LkSpacing.lg  = 16.dp  // padding horizontal de tela, padding de botão
LkSpacing.xl  = 24.dp  // gap entre seções dentro de uma tela
LkSpacing.xxl = 32.dp  // separação entre blocos maiores, espaço de respiro
```

Tokens derivados: `LkSpacing.cardContent` = 16.dp; section gap = `LkSpacing.xl`.

**Proibido:** valores mágicos (ex.: `padding = 13.dp`). Usar os tokens ou justificar explicitamente.

---

### Bordas e raio

```kotlin
LkRadius.card   = 16.dp
LkRadius.button = 12.dp
LkRadius.input  = 12.dp
```

Chips: `999.dp` (pill shape — `RoundedCornerShape(999.dp)`).

---

### Estrutura de tela (padrão)

```kotlin
@Composable
fun NomeDaScreen() {
    val tokens = LocalLkTokens.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Título", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tokens.bgPrimary),
            )
        },
        containerColor = tokens.bgPrimary,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xl),
        ) {
            // seções
        }
    }
}
```

### Estrutura de card (padrão)

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(LkRadius.card),
    colors = CardDefaults.cardColors(containerColor = tokens.bgCard),
    border = BorderStroke(1.dp, tokens.border),
) {
    Column(
        modifier = Modifier.padding(LkSpacing.cardContent),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        // conteúdo
    }
}
```

---

### Princípios de layout

1. Uma métrica principal por tela.
2. Hierarquia clara: título → métrica → corpo → label.
3. Contraste WCAG AA (mínimo 4.5:1 em texto normal).
4. Estado visível por cor + ícone — nunca só cor.
5. Transições curtas: 200–220 ms para push/modal.
6. Respiro entre seções: `LkSpacing.xl`.
7. Densidade média: card padding via `LkSpacing.cardContent`.
8. Branding silencioso: logo em splash, corner do header e footer.

---

### Convenções de nomenclatura de arquivos

| Tipo | Padrão | Exemplo |
|---|---|---|
| Tela completa | `{Feature}Screen.kt` | `DiagnosticoScreen.kt` |
| Componente reutilizável | `{ComponentName}.kt` | `GaugeCircular.kt` |
| Composable de seção | `{Feature}Section.kt` | `RedeSection.kt` |

---

## Sua tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `create <NomeDaScreen>`

1. Pergunte: propósito da tela, quais dados exibe, quais ações o usuário toma.
2. Identifique quais componentes existentes (`ui/component/`) podem ser reutilizados.
3. Gere Compose completo seguindo Scaffold + LazyColumn, `LocalLkTokens.current`, `LkSpacing.*`, `MaterialTheme.typography.*`, `LkRadius.*`.
4. Explique as decisões de design.

### Modo `review <arquivo.kt>`

1. Leia o arquivo.
2. Verifique contra as regras: hex literal, valor mágico de espaçamento, token legado, hierarquia tipográfica incorreta, ausência de `LocalLkTokens.current`.
3. Gere relatório com linha, problema e correção sugerida.
4. Pergunte se quer aplicar as correções.

### Modo `tokens`

Exiba uma tabela de referência rápida com todos os tokens disponíveis e seus valores atuais lidos do `SignallQTheme.kt`.

### Sem argumento — modo consultor

Pergunte ao usuário se está criando algo novo, revisando tela existente, ou com dúvida sobre qual token usar.

## Agentes canônicos ([ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md))

Design system é conduzido pela **Juliana**; implementação em Compose por **Camilo**; revisão independente por **Caio**.
