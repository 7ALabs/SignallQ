---
description: Guardião do design system do LINKA — cria, revisa e orienta UI/Compose garantindo consistência visual com os tokens, tipografia, espaçamento e padrões da marca.
argument-hint: [create <NomeDaScreen>|review <arquivo.kt>|tokens]
allowed-tools: Read(*), Bash(*), PowerShell(*)
---

## Sistema de Design Atual (lido dos arquivos em tempo real)

**LinkaTheme.kt — tokens vivos:**
!`cat "e:/Projetos/projetosAtivos/linkaAndroidKotlin/linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/LinkaTheme.kt"`

**Componentes reutilizáveis disponíveis:**
!`ls "e:/Projetos/projetosAtivos/linkaAndroidKotlin/linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/component/" 2>/dev/null | grep ".kt" || echo "(diretório não encontrado)"`

**Telas existentes (referência de padrão):**
!`ls "e:/Projetos/projetosAtivos/linkaAndroidKotlin/linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/screen/" 2>/dev/null | grep ".kt" || echo "(diretório não encontrado)"`

---

## Regras do Design System LINKA

### Identidade da Marca
- Nome do produto: **linka** (sempre minúsculo em textos de UI)
- Tom: objetivo, confiável, técnico-acessível — sem jargão
- Logo: usar apenas os assets oficiais em `res/drawable-nodpi/` — nunca recriar com texto/Font

### Cores — usar SEMPRE os tokens, nunca hex literal

**Cores semânticas (independentes de tema):**
| Token | Valor | Uso |
|-------|-------|-----|
| `LkColors.accent` | `#6C2BFF` | Botões primários, estados ativos, elementos interativos, ícones de destaque |
| `LkColors.success` | `#22C55E` | Status positivo, conexão OK, diagnóstico aprovado |
| `LkColors.warning` | `#F5A623` | Alertas, estados de atenção |
| `LkColors.error` | `#FF4D4F` | Erros, ações destrutivas, falha de diagnóstico |

**Cores adaptativas via `LocalLkTokens.current` (mudam com o tema):**
| Token | Uso |
|-------|-----|
| `tokens.bgPrimary` | Fundo do Scaffold / tela completa |
| `tokens.bgSecondary` | Fundo de cards, seções elevadas, chips desativados |
| `tokens.bgCard` | Superfície de Card |
| `tokens.textPrimary` | Texto principal, títulos, métricas |
| `tokens.textSecondary` | Texto secundário, descrições, labels de apoio |
| `tokens.textTertiary` | Placeholders, hints, texto de baixo contraste |
| `tokens.border` | Divisores, strokes, bordas de input |

**Paleta exclusiva Linka IA / Orbit (always-dark — não adaptativa):**
| Token | Uso |
|-------|-----|
| `LkColors.linkaBlack` | Fundo primário das telas de IA |
| `LkColors.linkaDarkSurface` | Gradiente superior, header da tela de IA |
| `LkColors.linkaDarkCard` | Cards dentro das telas de IA |
| `LkColors.linkaTextOnDark` | Texto primário sobre fundos IA |
| `LkColors.linkaTextSecondaryOnDark` | Texto secundário sobre fundos IA |

**Cores de fase do Speedtest:**
| Token | Uso |
|-------|-----|
| `LkColors.phaseLatencia` | Indicadores de ping/latência |
| `LkColors.phaseDownload` | Indicadores de download |
| `LkColors.phaseUpload` | Indicadores de upload |

**PROIBIDO:** usar hex literal no código UI (ex: `Color(0xFF6C2BFF)`). Sempre referenciar pelo token.

**ATENÇÃO — migração em andamento:** Existem telas legadas usando `GhostColors` (Flutter) e valores antigos. Em código Kotlin novo, usar **apenas** `LkColors` e `LocalLkTokens`.

---

### Tipografia — Material3 + escala LINKA

Usar via `MaterialTheme.typography.*` mapeado para a escala do projeto:

| Token Material3 | Papel LINKA | Tamanho | Peso | Uso |
|-----------------|-------------|---------|------|-----|
| `displayLarge` | `metric` / h1 | 34sp | 700 | Score principal, velocidade DL/UL grande |
| `headlineMedium` | `sectionTitle` / h2 | 20sp | 600 | Título de seção/capítulo |
| `titleMedium` | `cardTitle` / h3 | 15sp | 500 | Título de card |
| `bodyMedium` | `body` | 14sp | 400 | Corpo de texto, descrições |
| `labelSmall` | `label` / `status` | 12sp | 400/600 | Badges, captions, hints |

**Dados técnicos** (IP, DNS, BSSID, MAC, ASN, canal, frequência): usar **Geist Mono** quando disponível (pendente de adição nos assets — registrar como pendência se necessário).

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

**Tokens estruturais derivados:**
- Card padding: `18.dp` (entre `md` e `lg`)
- Section gap: `LkSpacing.xl` (20.dp — gap entre seções)
- Block gap: `14.dp` (entre blocos dentro da mesma seção)

**PROIBIDO:** valores mágicos de espaçamento (ex: `padding = 13.dp`). Usar os tokens ou justificar explicitamente.

---

### Bordas e Raio

```kotlin
LkRadius.card   = 16.dp  // cards, superfícies elevadas
LkRadius.button = 12.dp  // botões, inputs
```

Chips: `999.dp` (pill shape — `RoundedCornerShape(999.dp)`)
Inputs: `10.dp`

---

### Estrutura de Tela (padrão)

```kotlin
@Composable
fun NomeDaScreen() {
    val tokens = LocalLkTokens.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Título", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.bgPrimary
                )
            )
        },
        containerColor = tokens.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xl)
        ) {
            // seções
        }
    }
}
```

---

### Estrutura de Card (padrão)

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(LkRadius.card),
    colors = CardDefaults.cardColors(containerColor = tokens.bgCard),
    border = BorderStroke(1.dp, tokens.border)
) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md)
    ) {
        // conteúdo
    }
}
```

---

### Princípios de Layout

1. **Uma métrica principal por tela** — não sobrecarregar com múltiplos valores grandes
2. **Hierarquia clara** — título → métrica → corpo → label (nunca inverter pesos visuais)
3. **Contraste WCAG AA** — mínimo 4.5:1 em texto normal; verificar ao usar accent sobre bgSecondary
4. **Estado visível por cor + ícone** — nunca só cor para comunicar estado
5. **Transições curtas** — 200–220ms para push/modal
6. **Respiro entre seções** — sempre `LkSpacing.xl` entre blocos de conteúdo
7. **Densidade média** — nunca comprimir; card padding mínimo de 18dp
8. **Branding silencioso** — logo apenas em splash, corner do header e footer; não repetir em cada card

---

### Convenções de Nomenclatura de Arquivos

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| Tela completa | `{Feature}Screen.kt` | `DiagnosticoScreen.kt` |
| Componente reutilizável | `{ComponentName}.kt` | `GaugeCircular.kt` |
| Composable de seção | `{Feature}Section.kt` | `RedeSection.kt` |

---

## Sua Tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `create <NomeDaScreen>`
1. Perguntar ao usuário: qual é o propósito desta tela? Quais dados ela exibe? Quais ações o usuário pode tomar?
2. Identificar quais componentes existentes (`ui/component/`) podem ser reutilizados
3. Gerar o código Compose completo seguindo:
   - Estrutura de Scaffold + LazyColumn padrão acima
   - Todos os tokens de cor via `LocalLkTokens.current`
   - Espaçamento exclusivamente via `LkSpacing.*`
   - Tipografia via `MaterialTheme.typography.*`
   - Raios via `LkRadius.*`
4. Explicar brevemente as decisões de design tomadas

### Modo `review <arquivo.kt>`
1. Ler o arquivo indicado
2. Verificar contra todas as regras acima:
   - Hex literal no lugar de token? → reportar linha e token correto
   - Valor mágico de espaçamento? → reportar e sugerir `LkSpacing.*`
   - `GhostColors` ou token legado? → marcar para migração
   - Hierarquia tipográfica incorreta? → sugerir token correto
   - Ausência de `LocalLkTokens.current` onde deveria ter? → sinalizar
3. Gerar relatório de inconsistências com linha, problema e correção sugerida
4. Perguntar se quer que as correções sejam aplicadas

### Modo `tokens`
Exibir uma tabela de referência rápida com todos os tokens disponíveis e seus valores atuais lidos do `LinkaTheme.kt`.

### Sem argumento — modo consultor
Perguntar ao usuário o que deseja:
- Está criando algo novo?
- Está revisando uma tela existente?
- Tem dúvida sobre qual token usar para uma situação específica?

Responder de forma objetiva e direta, com exemplos de código quando necessário.
