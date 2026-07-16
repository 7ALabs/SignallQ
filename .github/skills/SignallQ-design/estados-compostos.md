# SignallQ — Estados Compostos de UI

> Fonte de verdade para estados de tela. Todo estado novo deve mapear para um dos padrões
> abaixo antes de ser implementado. Nenhum estado deve ser inventado localmente por feature.

---

## Princípios gerais

- Estado de UI é comunicação. O usuário precisa saber o que está acontecendo e o que fazer.
- Hierarquia: conteúdo > erro > vazio > loading. Quando há conteúdo, ele prevalece.
- Copy em PT-BR, sentence case, sem jargão técnico, sem emoji.
- Toda ação visível precisa ter um resultado previsível para o usuário.
- Componente genérico `StatefulScreen` cobre os casos puros (Loading / Empty / Error / Success).
  Estados compostos exigem tratamento na tela específica.

---

## Estados puros

### `loading`

**Nome canônico:** `UiState.Loading`

**Quando ocorre no SignallQ:**
- Histórico carregando registros do Room pela primeira vez
- DiagnosticoScreen aguardando dados de rede
- DispositivosScreen na varredura inicial de dispositivos
- DnsScreen aguardando resolução DNS

**Regra de escolha entre spinner, skeleton e shimmer:**

| Situação | Padrão | Motivo |
|---|---|---|
| Tela inteira aguarda um dado central desconhecido | `CircularProgressIndicator` centralizado | Não há layout para preservar |
| Card ou bloco tem estrutura conhecida | Skeleton shimmer (retângulos animados) | Preserva hierarquia visual, reduz layout shift |
| Refresh sobre conteúdo existente | `PullToRefreshBox` indicator nativo | O conteúdo já está visível, o indicator é sutil |
| Ação em botão (submit, retry) | Indicador no botão ou desabilitar + spinner inline | Não bloqueia a tela toda |

**O que exibir:**
- Spinner: `CircularProgressIndicator`, cor `LkColors.accent`, strokeWidth 3dp, tamanho 40dp, centralizado
- Semantics: `contentDescription = "Carregando"` (já no `StatefulScreen`)
- Skeleton: retângulos com shimmer animado (`LinearEasing`, 1000ms, RepeatMode.Restart), cor `c.border @50%` + `Color.White @15%` (padrão do `DnsSkeletonBloco1`)

**O que NÃO fazer:**
- Não usar spinner em tela que já tem conteúdo parcial — use skeleton ou `content + loading`
- Não mostrar loading sem semantics (invisível para acessibilidade)
- Não bloquear interações que não dependem do dado que está carregando
- Não usar `shimmer` em blocos sem estrutura prévia definida

**Tokens:**
- `LkColors.accent` para o spinner
- `c.border` para o shimmer base
- `LkSpacing.xxl` padding horizontal no container centralizado

---

### `empty`

**Nome canônico:** `UiState.Empty`

**Quando ocorre no SignallQ:**
- Histórico sem nenhum teste realizado
- Dispositivos sem nenhum dispositivo encontrado na rede
- Lista de redes Wi-Fi vazia (Wi-Fi desligado ou sem redes ao alcance)

**Variante com ação primária:**
Usar quando o usuário pode resolver o vazio com uma ação direta no app.

O que exibir:
- Ícone em 56dp, cor `c.textTertiary` (Material Symbols Outlined)
- Título em `titleLarge`, cor `c.textPrimary`, texto centrado, sentence case
- Subtítulo em `bodyMedium`, cor `c.textSecondary`, texto centrado
- `OutlinedButton` com label da ação (não `FilledButton` — vazio não é CTA principal)
- Espaçamento entre ícone e título: `LkSpacing.lg`; entre título e subtítulo: `LkSpacing.sm`; antes do botão: `LkSpacing.xl`

**Variante sem ação primária:**
Usar quando o vazio é informacional e não há ação imediata possível (ex.: histórico vazio antes do primeiro teste).

O que exibir: ícone + título + subtítulo. Sem botão.

**Copy padrão:**
- Título: "Nada por aqui" (genérico) ou específico da feature ("Nenhum teste realizado")
- Subtítulo: explicação breve do porquê e o que fazer fora do app se aplicável

**O que NÃO fazer:**
- Não usar `FilledButton` em estado vazio — reservado para erro
- Não usar ícone colorido em estado vazio — `textTertiary` sinaliza ausência, não problema
- Não deixar estado vazio sem subtítulo — o usuário precisa de contexto

**Tokens:**
- `c.textTertiary` para o ícone
- `c.textPrimary` / `c.textSecondary` para os textos
- `LkSpacing.xxl` padding horizontal do container

---

### `error` — recuperável

**Nome canônico:** `UiState.Error` com `onRetry`

**Quando ocorre no SignallQ:**
- DiagnosticoScreen: falha na IA (timeout, 503/504, erro de rede)
- FibraModemScreen: falha ao conectar ao modem
- DispositivosScreen: erro de rede durante varredura

**O que exibir:**
- Ícone `Icons.Outlined.ErrorOutline`, 56dp, cor `LkColors.error`
- Título fixo: "Algo deu errado"
- Mensagem: texto sanitizado pelo ViewModel (nunca stack trace)
- Botão primário `FilledButton`: "Tentar novamente", cor `LkColors.accent`

**O que NÃO fazer:**
- Não expor código técnico de erro diretamente — mapear para código amigável (ex.: `ERR_TIMEOUT`)
- Não omitir o botão de retry em erro recuperável
- Não usar cor de warning (`#F5A623`) para erro — usar `LkColors.error`

**Tokens:**
- `LkColors.error` para o ícone
- `LkColors.accent` para o botão de retry

---

### `error` — fatal

**Nome canônico:** `UiState.Error` sem retry disponível (ex.: `SemCredenciais`, `SemWifi`)

**Quando ocorre no SignallQ:**
- FibraModemUiState.SemWifi — feature requer Wi-Fi e não há conexão
- FibraModemUiState.SemCredenciais — configuração ausente, impossível prosseguir

**O que exibir:**
- Ícone representativo da causa (ex.: `WifiOff`, `Lock`)
- Título específico descrevendo o bloqueio
- Subtítulo com próximo passo externo ao app (ex.: conectar ao Wi-Fi, configurar nas ajustes)
- Sem botão de retry. CTA secundário apenas se houver ação possível (ex.: abrir Ajustes do Android)

**O que NÃO fazer:**
- Não mostrar botão "Tentar novamente" quando não há o que tentar
- Não usar o mesmo ícone `ErrorOutline` de erro recuperável — diferencia visualmente
- Não deixar o usuário sem próximo passo

---

## Estados compostos

### `empty + loading`

**Nome canônico:** `loading-first-fetch`

**Quando ocorre no SignallQ:**
- DispositivosScreen na primeira varredura de rede (nenhum dispositivo ainda, escaneando)
- DnsScreen na primeira resolução DNS
- HistoricoScreen no carregamento inicial do banco

**O que exibir:**
- Spinner centralizado (`UiState.Loading`) — não skeleton, porque não há estrutura prévia para preservar
- Copy opcional inline abaixo do spinner: "Buscando..." (curto, sem ponto, sem emoji)
- NÃO mostrar estado vazio durante o loading — o vazio só aparece após a resposta confirmar ausência de dados

**O que NÃO fazer:**
- Não flashar estado `empty` antes de confirmar que o dado é realmente vazio
- Não usar skeleton quando o layout dos items não é conhecido

**Tokens:**
- Mesmo do `loading` puro

---

### `content + loading`

**Nome canônico:** `content-refreshing`

**Quando ocorre no SignallQ:**
- DispositivosScreen com `PullToRefreshBox` — lista existe, usuário puxou para atualizar
- SinalScreen refresh manual — redes já listadas, re-escaneando
- HomeScreen atualizando snapshot de rede em background

**O que exibir:**
- Conteúdo existente permanece visível e navegável
- `PullToRefreshBox` com indicator nativo do M3 (já implementado em `DispositivosScreen`)
- Para refresh silencioso (sem pull): `LinearProgressIndicator` no topo da tela, cor `LkColors.accent`, sem alpha, sem texto de status

**O que NÃO fazer:**
- Não substituir o conteúdo por spinner durante refresh — o usuário perde o contexto
- Não desabilitar interações durante refresh (ex.: swipe, tap em cards)
- Não mostrar "Atualizando..." em texto — o indicator é suficiente

**Tokens:**
- `LkColors.accent` para `LinearProgressIndicator`
- Indicator do `PullToRefreshBox` segue o padrão M3 nativo

---

### `content + error`

**Nome canônico:** `partial-error`

**Quando ocorre no SignallQ:**
- DiagnosticoScreen: diagnóstico local OK, IA falhou — conteúdo local visível, erro de IA em dialog
- FibraModemScreen com GPON ok mas WAN com erro parcial
- HomeScreen com snapshot de rede ok mas seção de velocidade sem dados

**O que exibir:**
- Conteúdo principal visível e funcional
- Erro parcial como `AlertDialog` (quando a falha é da IA/serviço externo e o diagnóstico local existe)
  - Título: "IA temporariamente indisponível" (exemplo real de `DiagnosticoScreen`)
  - Body: descrição amigável da falha + nota de que o restante funciona
  - CTA primário: "Tentar novamente"
  - CTA secundário: dismiss/fechar
- Alternativa para falhas silenciosas: banner inline `OfflineBanner` ou card de aviso com cor `warning @12%` fill

**O que NÃO fazer:**
- Não ocultar o conteúdo que funcionou por causa da falha parcial
- Não usar toast/snackbar para erros que requerem ação — usar dialog ou banner fixo
- Não expor código de erro no title do dialog — usar código amigável (`ERR_TIMEOUT`, `ERR_SERVIDOR_INDISPONIVEL`)

**Tokens:**
- `LkColors.warning @12%` fill para banners de aviso parcial
- `LkColors.error` para ícones de erro
- `LkColors.accent` para CTA de retry

---

### `loading timeout`

**Nome canônico:** `loading-timeout`

**Quando ocorre no SignallQ:**
- ChatDiagnosticoIaScreen: IA não respondeu no tempo esperado (`codigoErro = "timeout"`)
- DiagnosticoScreen: `AiDiagnosisState.timeout` → `UiState.Error("timeout")`

**Threshold:** definido pelo ViewModel/repositório (não hardcoded na UI). O timeout visível deve ser comunicado quando a operação demorar mais do que o esperado — a UI não define o limite, apenas reage ao estado `timeout`.

**O que exibir:**
Quando o estado `timeout` chega à UI:
- Se for erro total (sem conteúdo local): `UiState.Error` com mensagem "A análise demorou mais do que esperado. Verifique sua conexão e tente novamente."
- Se for erro parcial (conteúdo local disponível): `AlertDialog` — "A IA não respondeu agora. O diagnóstico local continua funcionando." + botão "Tentar novamente"

**O que NÃO fazer:**
- Não mostrar "Aguardando..." indefinidamente sem limite — o estado timeout fecha o loop
- Não expor "timeout" como texto para o usuário — traduzir para linguagem humana
- Não travar a UI em loading após o timeout — sempre resolver para erro ou fallback

**Tokens:**
- Mesmo do `error` recuperável

---

### `offline`

**Nome canônico:** `offline-no-cache` / `offline-with-cache`

**Quando ocorre no SignallQ:**
- `OfflineBanner` exibido no topo de qualquer tela quando não há conexão ativa
- HomeScreen com `EstadoConexao` indicando ausência de rede
- SpeedTest tentado sem conexão

**Variante sem cache:**
- Banner `OfflineBanner` no topo (já implementado): fundo `warning @12%`, ícone `WifiOff` 16dp `warning`, texto "Sem conexão ativa" 13sp W500 `warning`
- Se a feature não funciona offline: mostrar estado vazio com subtítulo "Você está sem conexão. Reconecte e tente novamente."
- Sem botão de retry — o usuário precisa reconectar primeiro

**Variante com cache:**
- Banner `OfflineBanner` no topo (sinaliza o estado)
- Conteúdo cacheado visível com marcação de data/hora da última atualização em `body-small`, `textTertiary`
- Interações que requerem rede são desabilitadas com `enabled = false`

**O que NÃO fazer:**
- Não esconder o banner quando há cache — o usuário precisa saber que está offline
- Não usar cor de erro (`#FF4D4F`) para offline — é `warning` (`#F5A623`), não é falha
- Não desabilitar toda a UI quando há cache disponível

**Tokens:**
- `LkColors.warning` e `LkColors.warning @12%` (padrão do `OfflineBanner`)
- `c.textTertiary` para timestamp de último dado

---

### `permission denied`

**Nome canônico:** `permission-denied-requestable` / `permission-denied-permanent`

**Quando ocorre no SignallQ:**
- Localização negada: `PermissaoLocalizacaoContextoSheet` (SinalScreen, DispositivosScreen)
- Telefonia negada: `PermissaoTelefoniaContextoSheet` (SinalScreen — dados móveis)
- `localizacaoBloqueadaPermanentemente: Boolean` diferencia as variantes

**Variante requestable (pode solicitar):**
- `ModalBottomSheet` com ícone do recurso (ex.: `LocationOn`), título explicativo ("Por que precisamos da localização?"), body explicando sem jargão, dois botões em Row:
  - TextButton: "Agora não" (dismiss)
  - FilledButton: "Entendi, conceder" (`LkColors.accent`)
- Sem ícone de erro — usar ícone do recurso em `LkColors.accent`

**Variante permanente (bloqueada nas configurações do Android):**
- Mesmo `ModalBottomSheet`
- Título: "Permissão bloqueada"
- Body: "A permissão foi bloqueada nas configurações do Android. Para ativar, abra os ajustes do app."
- Botão primário: "Abrir ajustes do Android" (abre `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`)
- Botão secundário TextButton: "Agora não"

**O que NÃO fazer:**
- Não mostrar estado de erro (`ErrorOutline`) para permissão — é um gate de configuração, não falha
- Não bloquear toda a tela — usar `ModalBottomSheet` para não perder contexto
- Não pedir permissão sem explicar o porquê antes (contexto obrigatório no body)
- Não looping de solicitação após negativa — mostrar variante permanente na segunda negativa

**Tokens:**
- `LkColors.accent` para ícone e botão primário
- `c.textSecondary` para o botão "Agora não"
- `ModalBottomSheet` padrão M3 com `SheetDragHandle`

---

## Referência rápida — tabela de decisão

| Situação | Estado canônico | Componente principal |
|---|---|---|
| Primeira carga, sem dados ainda | `loading-first-fetch` | `CircularProgressIndicator` centralizado |
| Primeira carga de bloco com layout conhecido | `loading` com skeleton | `DnsSkeletonBloco1` como referência |
| Carga concluída, sem dados | `UiState.Empty` | `StatefulScreen` |
| Erro recuperável, sem conteúdo | `UiState.Error` + retry | `StatefulScreen` |
| Erro fatal / bloqueio estrutural | `UiState.Error` sem retry | Tela dedicada ou composable inline |
| Refresh sobre conteúdo existente | `content-refreshing` | `PullToRefreshBox` ou `LinearProgressIndicator` |
| Serviço externo falhou, conteúdo local ok | `partial-error` | `AlertDialog` |
| IA/rede não respondeu no tempo | `loading-timeout` | Resolve para `UiState.Error` ou `AlertDialog` |
| Sem internet, feature não funciona offline | `offline-no-cache` | `OfflineBanner` + empty |
| Sem internet, cache disponível | `offline-with-cache` | `OfflineBanner` + conteúdo com data |
| Permissão não concedida, pode solicitar | `permission-denied-requestable` | `ModalBottomSheet` |
| Permissão bloqueada permanentemente | `permission-denied-permanent` | `ModalBottomSheet` com "Abrir ajustes" |

---

## Componentes existentes — não recriar

| Componente | Localização | Cobre |
|---|---|---|
| `StatefulScreen` | `ui/component/StatefulScreen.kt` | `loading`, `empty`, `error` puros |
| `OfflineBanner` | `ui/component/OfflineBanner.kt` | offline banner |
| `PermissaoLocalizacaoContextoSheet` | `ui/screen/PermissaoLocalizacaoContextoSheet.kt` | permission denied (localização) |
| `PermissaoTelefoniaContextoSheet` | `ui/screen/PermissaoTelefoniaContextoSheet.kt` | permission denied (telefonia) |
| `PullToRefreshBox` | M3 nativo (`androidx.compose.material3.pulltorefresh`) | content-refreshing |
| `DnsSkeletonBloco1` | `ui/screen/DnsScreen.kt` (privado) | skeleton shimmer de referência |

---

## Microcopy — referência rápida

| Estado | Título | Subtítulo / body |
|---|---|---|
| loading genérico | — | — (spinner fala por si) |
| loading inline | — | "Buscando..." |
| empty genérico | "Nada por aqui" | "Nenhum dado disponível no momento." |
| empty histórico | "Nenhum teste realizado" | "Faça seu primeiro teste de velocidade para ver os resultados aqui." |
| empty dispositivos | "Nenhum dispositivo encontrado" | "Aguarde alguns segundos e tente novamente." |
| error recuperável | "Algo deu errado" | mensagem sanitizada do ViewModel |
| error IA timeout | "IA temporariamente indisponível" | "A IA não respondeu agora. O diagnóstico local continua funcionando." |
| error timeout total | "Análise demorou demais" | "Verifique sua conexão e tente novamente." |
| offline sem cache | "Sem conexão" | "Você está sem conexão. Reconecte e tente novamente." |
| offline com cache | (banner topo) | timestamp "Último dado: [data]" |
| sem Wi-Fi (feature) | "Wi-Fi necessário" | "Esta funcionalidade requer Wi-Fi. Conecte-se a uma rede e volte." |
| permissão requestable | "Por que precisamos da localização?" | explicação contextual sem jargão |
| permissão bloqueada | "Permissão bloqueada" | "A permissão foi bloqueada nas configurações do Android. Para ativar, abra os ajustes do app." |
