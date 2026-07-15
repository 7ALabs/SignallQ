# Design Specs — ChatDiagnosticoIaScreen (entregue pela Lia)

> Especificação acionável para Camilo implementar na Etapa 4 (Screen + componentes).
> Microcopy PT-BR final — não inventar texto novo.
> Tokens SignallQ via `LocalLkTokens.current` e `MaterialTheme.colorScheme`.

---

## Topbar

```
CenterAlignedTopAppBar(
  title = "Diagnóstico IA" (titleLarge),
  navigationIcon = BackArrow,    // esquerda
  actions = IconButton(Icons.Outlined.History)   // direita — abre drawer
  containerColor = surface
)
```

Sem ícone decorativo de IA no título.

---

## Estrutura

```
ModalNavigationDrawer (drawer à esquerda)
  └── Scaffold (topbar; bottomBar VAZIO durante executandoTeste)
        └── Column(windowInsetsPadding(WindowInsets.ime))
              ├── LazyColumn (mensagens + OpcoesIniciaisChips inline)
              └── SignallQInputArea (passa chips = emptyList())
                  ↪ substituído por CotaExcedidaBanner quando cota=excedida
```

---

## OpcoesIniciaisChips (vai DENTRO do LazyColumn, abaixo da 2ª mensagem da IA)

- 3 botões em coluna vertical (não LazyRow).
- `FilledTonalButton` MD3, `fillMaxWidth().heightIn(min = 48.dp)`.
- `shape = RoundedCornerShape(LkRadius.button)` (12.dp).
- `colors = filledTonalButtonColors(containerColor = secondaryContainer, contentColor = onSecondaryContainer)`.
- `typography = labelLarge`, `textAlign = Start`, padding horizontal = `LkSpacing.lg`.
- Column padding: `start = 36.dp, end = LkSpacing.lg, top = LkSpacing.sm` — alinha com conteúdo do bubble da IA.
- `verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)`.
- Após escolha, todos ficam `enabled = false` e somem na próxima recomposição (ViewModel controla `opcoesMostradas: Boolean`).
- `contentDescription = chip.label`.

**Textos exatos:**
- `Analisar meu último teste`
- `Executar novo teste agora`
- `Analisar meu histórico recente`

---

## MensagensProgressoTeste

Não é componente — é comportamento do ViewModel. Reusa `SignallQAiMessageBubble` com novo parâmetro `isProgressMessage: Boolean = false` que suprime:
- statusLabel pill
- métricas (download/upload/latência inline)
- BulletActionList
- "Ver detalhes técnicos"

`sourceLabel = "Diagnóstico IA · HH:MM"`. `isLatest = true` durante streaming (TypewriterText).

---

## CotaExcedidaBanner (substitui o input quando estado=cotaExcedida)

```
Surface(fillMaxWidth, color = tokens.warningContainer)
  Column(padding = horizontal LkSpacing.lg, vertical LkSpacing.md, spacedBy LkSpacing.xs)
    Text("Limite diário atingido", titleSmall, onWarningContainer, Bold)
    Text("Você poderá fazer uma nova análise em ${renewalDateTime}.", bodySmall, onWarningContainer)
```

Altura mínima 56.dp. Sem botão. `wrapContentHeight()` + padding generoso para `fontScale 1.3+`.

---

## SessaoListItem (drawer)

```
NavigationDrawerItem(
  selected = sessao.id == sessaoAtualId,
  onClick = onAbrirSessao,
  label = Column(spacedBy 2.dp) {
    Text(sessao.titulo, titleSmall, maxLines = 1, overflow = Ellipsis)
    Text(dataFormatada, labelMedium, color = tokens.textTertiary)
  },
  modifier = padding(horizontal = LkSpacing.sm)
)
```

- Data relativa: `"Hoje, 14:32"` / `"Ontem"` / `"DD/MM"` (sem ano se mesmo ano).
- SEM preview de última mensagem.
- Highlight do selected: confiar no MD3 (`NavigationDrawerItem` já aplica).
- Long-press abre `DropdownMenu` com "Renomear" e "Apagar" (sem ícones nos itens).
- `contentDescription = "${sessao.titulo}, ${dataFormatada}"`.

---

## Drawer

- Largura: default MD3 (`ModalDrawerSheet` sem largura customizada = 360dp).
- `Modifier.semantics { paneTitle = "Histórico de conversas" }` no `ModalDrawerSheet`.

**Cabeçalho:**
```
Column(padding = horizontal LkSpacing.lg, vertical LkSpacing.xl)
  Text("Conversas", headlineSmall, tokens.textPrimary)
  Spacer(LkSpacing.md)
  OutlinedButton("Nova conversa", fillMaxWidth, height = 48.dp, RoundedCornerShape(LkRadius.button))
    Icon(Icons.Outlined.Add, size = 18.dp) + Spacer(LkSpacing.sm) + Text(labelLarge)
HorizontalDivider(color = tokens.border)
```

**Lista:** LazyColumn de `SessaoListItem` ordenados por `atualizadoEmEpochMs DESC`.

**Estado vazio:**
```
Box(fillMaxWidth, padding LkSpacing.xl, contentAlignment = Center) {
  Text("Nenhuma conversa anterior.", bodyMedium, tokens.textTertiary, textAlign = Center)
}
```

---

## DialogConfirmarApagar

```
AlertDialog(
  title = "Apagar conversa?",
  text = "Esta ação não pode ser desfeita.",
  confirmButton = TextButton("Apagar", color = error),
  dismissButton = TextButton("Cancelar")
)
```
Sem ícone.

---

## DialogRenomearSessao

```
AlertDialog(
  title = "Renomear conversa",
  content = OutlinedTextField(
    value = novoTitulo (pre-preenchido com título atual),
    onValueChange = { if (it.length <= 60) novoTitulo = it },
    label = "Nome da conversa",
    singleLine = true,
    supportingText = "${novoTitulo.length}/60"  // sempre visível
  ),
  confirmButton = TextButton("Salvar", enabled = novoTitulo.isNotBlank()),
  dismissButton = TextButton("Cancelar")
)
```

---

## Microcopy PT-BR (textos exatos — não inventar)

### Boas-vindas (primeira mensagem da IA)

> Olá. Sou o Diagnóstico IA do SignallQ.
>
> Posso ajudar você a entender problemas de internet, Wi-Fi, velocidade, latência, perda de pacote e qualidade da sua rede. Trabalho apenas com assuntos relacionados à sua conexão — não sou um assistente geral e posso cometer erros. Use minhas respostas como apoio, não como verdade absoluta.

### Segunda mensagem da IA

> Como você quer começar?

### Progresso do novo teste (4 mensagens)

1. `Iniciando o teste. Primeiro vou medir a velocidade de download.`
2. `Download: ${velocidadeDownload} Mbps. Agora medindo o upload.` — fallback: `Download concluído. Medindo upload agora.`
3. `Upload: ${velocidadeUpload} Mbps. Verificando latência, estabilidade e outros sinais da rede.` — fallback: `Upload concluído. Verificando latência e estabilidade.`
4. `Dados coletados. Analisando com ${modelDisplayName}.` — fallback de display: `o modelo de IA` (NUNCA hardcode "Gemma 4").

### Erros (5 cenários)

- **Modelo indisponível (503):** `No momento o ${modelDisplayName} está indisponível. Tente novamente em alguns minutos.`
- **Sem rede:** `Não consegui conectar ao serviço de diagnóstico. Verifique sua conexão e tente novamente.`
- **Timeout:** `A análise demorou mais que o esperado e foi interrompida. Você pode tentar novamente — os dados do teste foram preservados.`
- **Resposta incompleta:** `Recebi uma resposta incompleta. Os dados do teste foram preservados, mas recomendo tentar o diagnóstico novamente.`
- **Catch-all:** `Algo deu errado ao processar o diagnóstico. Tente novamente. Se o problema persistir, os dados do teste foram salvos e você pode tentar mais tarde.`

### Cota excedida

- Título: `Limite diário atingido`
- Texto: `Você poderá fazer uma nova análise em ${renewalDateTime}.`
- Formato `renewalDateTime`: `amanhã às 14h32` / `hoje às 22h15` — `DateTimeFormatter` locale `pt-BR` + timezone local. Fallback: `em 24 horas`.

### Fora de escopo

> Só consigo ajudar com assuntos relacionados à sua conexão, Wi-Fi, testes de rede e diagnósticos do SignallQ. Quer que eu analise seu último teste ou ajude a entender um problema de internet?

---

## Título automático da sessão — DECISÃO FECHADA

**Primeira mensagem do usuário, truncada a 40 chars.**

Se a primeira interação for via chip (sem texto digitado), usar `Análise: ${chip.label}`.

Implementação:
```kotlin
titulo = mensagem.take(40).trimEnd() + if (mensagem.length > 40) "…" else ""
```

---

## Comportamento crítico

- **Auto-scroll inteligente:** só `animateScrollToItem(lastIndex)` se o usuário já estava no fundo (`lazyListState.isScrolledToEnd()`). NÃO puxar usuário de volta enquanto ele lê histórico.
- **Input durante teste:** `SignallQInputArea.enabled = false` durante `executandoTeste` e `aguardandoIa`. Placeholder: `Aguarde o resultado do teste...`.
- **Bottom nav:** oculta durante `executandoTeste` (mesmo gate que ChatScreen atual usa).
- **TypewriterText em respostas longas (>500 chars):** verificar velocidade aceitável; se não, considerar fade-in por parágrafo.
- **Fontes grandes (fontScale 1.3+):** banner usa `wrapContentHeight()` + padding generoso.

---

## Tokens claro/escuro

Nada a customizar. Todos os tokens já têm variante dark:
- `tokens.bgSecondary` (bubble IA)
- `tokens.warningContainer` / `tokens.onWarningContainer` (banner)
- `MaterialTheme.colorScheme.secondaryContainer` (chips)

Verificar se `SignallQThinkingBubble` usa token (não hardcode).

---

## Acessibilidade

- Chips: `Modifier.semantics { contentDescription = chip.label }`.
- `SignallQThinkingBubble`: `contentDescription = "Diagnóstico IA está processando"`.
- `ModalDrawerSheet`: `Modifier.semantics { paneTitle = "Histórico de conversas" }`.
- DropdownMenuItem: `Modifier.semantics { contentDescription = "Renomear conversa ${sessao.titulo}" }`.
- Tamanhos de toque ≥48dp já garantidos pelo MD3 default.

---

## Arquivado — 2026-07-12

**Decisão do Luiz: sem chat com IA no produto.** Confirma e fecha a discussão aberta pelas
issues #215/#222 (chat já havia sido descontinuado da UI antes, código preservado por
enquanto) e #850 (confirmou zero ponto de entrada em produção). Esta spec bate 1:1 com o
código de `ChatDiagnosticoIaScreen.kt` — mas o código nunca teve navegação viva; era plumbing
órfão. Removido definitivamente via issue de limpeza (ver GitHub). Este arquivo fica só como
histórico de decisão de produto.
