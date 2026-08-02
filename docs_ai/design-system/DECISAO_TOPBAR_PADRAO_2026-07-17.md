# Decisão — Padrão único de TopBar (ícone no título + scroll behavior)

- **Status:** decidido, pronto para implementação
- **Última validação:** 2026-07-17
- **Fonte de verdade:** este documento (novas decisões que a spec/HANDOFF não cobria)
- **Escopo:** as 16 telas Android com `Scaffold(topBar = ...)` catalogadas na issue #1100
- **Responsável:** Lia (decisão) → Camilo (implementação)
- **Origem:** issue #1100 (Luiz) — "cada tela reagindo de uma forma ao scroll, algumas exibem
  ícone junto ao título e outras não"

Modo usado: **Sonnet** — decisão de padrão de UI que afeta 16 telas, não é revisão simples de
copy/contraste.

---

## Contexto verificado antes de decidir

O `HANDOFF_README.md` já define o layout fixo (`CenterAlignedTopAppBar`, avatar-como-leading nas
abas principais, seta-de-voltar nas telas secundárias) — isso **não muda** aqui, já está correto
em quase todas as telas. O que faltava decidir: (1) quando usar ícone junto ao título, (2) como a
TopBar reage a scroll, (3) o outlier do botão de Ajustes.

**Achado que muda a base da decisão 1 e 3:** o catálogo original da issue #1100 listava "Ajustes"
como uma das 5 abas do bottom nav (repetindo o que `CLAUDE.md` ainda diz). Isso está **desatualizado**.
Conferido em `AppShell.kt`:

```kotlin
// AppShell.kt:1055-1059 — AppBottomNavBar real hoje
AppNavItem(c, selectedTab, 0, "Início", "home", onTabSelected)
AppNavItem(c, selectedTab, 1, "Velocidade", "speed", onTabSelected, showBadge = testeAtivo)
AppNavItem(c, selectedTab, 2, "Sinal", "wifi", onTabSelected)
AppNavItem(c, selectedTab, 3, "Histórico", "history", onTabSelected)
AppNavItem(c, selectedTab, 4, "Ferramentas", "build", onTabSelected)
```

E o comentário em `AppShell.kt:887-890` confirma a razão: desde **GH#936** (Fase 7), Ajustes
deixou de ser aba e virou uma lista de entradas alcançada pelo avatar no TopBar (`Overlay.Perfil`),
substituída no bottom nav pela aba **Ferramentas**. As **5 abas reais hoje são: Início, Velocidade,
Sinal, Histórico, Ferramentas** — não "Ajustes".

**Isso é uma divergência de documentação a registrar, não a corrigir aqui** (fora do escopo desta
tarefa, afeta `CLAUDE.md` — seção Identidade — e potencialmente a seção Milestones/nav em outros
lugares). Sinalizado para Claudete/Rhodolfo no fechamento. A decisão abaixo já usa a topologia
real (5 abas = Início/Velocidade/Sinal/Histórico/Ferramentas), não a desatualizada.

---

## Decisão 1 — Ícone no título

**Regra fechada: ícone no título aparece exclusivamente nas 5 raízes de aba do bottom nav.
Nunca em tela secundária (toda tela alcançada por seta-de-voltar ou por overlay/avatar).**

Motivo: o próprio catálogo já mostrava isso quase acontecendo por acidente — 3 das 4 telas com
ícone hoje já são raízes de aba (Início, Sinal, Histórico); a 4ª (Dispositivos) é uma tela
secundária dentro do hub Ferramentas e é o único caso fora do padrão. Formalizar "ícone = raiz de
aba" resolve a inconsistência com a menor mudança possível e dá ao usuário um sinal consistente:
ver ícone junto ao título = você está numa das 5 seções principais do app, não dentro de um fluxo.

| Tela | Tem ícone hoje? | Decisão | Ícone (vector Compose) |
|---|---|---|---|
| Início | Sim | **Mantém** | `Icons.Outlined.Home` (já usado, `HomeScreen.kt:390`) |
| Velocidade (idle) | Não | **Ganha ícone** | `Icons.Outlined.Speed` |
| Velocidade (em andamento/erro) | Não | **Sem ícone** — título dinâmico ("Medindo…"/"Erro") already comunica estado, ícone fixo junto de um título que muda de texto compete por atenção. Não é regra "toda raiz de aba tem ícone sempre": estado transitório da própria aba abre exceção deliberada. | — |
| Sinal | Sim | **Mantém** | `Icons.Outlined.CellTower` (já usado, `SinalScreen.kt:370`) |
| Histórico | Sim | **Mantém** | `Icons.Outlined.History` (já usado, `HistoricoScreen.kt:352`) |
| Ferramentas | Não | **Ganha ícone** | `Icons.Outlined.Build` (mesmo glyph do bottom nav, `"build"`) |
| Ajustes (overlay, não é aba) | Não | **Sem ícone** — é overlay, segue regra de tela secundária | — |
| Dispositivos | Sim | **Perde ícone** (`Icons.Outlined.Devices` remove) — é tela secundária dentro de Ferramentas, único outlier a corrigir | — |
| Equipamento de Internet | Não | Sem ícone (já correto) | — |
| DNS | Não | Sem ícone (já correto) | — |
| Jogos | Não | Sem ícone (já correto) | — |
| Laudo | Não | Sem ícone (já correto) | — |
| Novidades | Não | Sem ícone (já correto) | — |
| Privacidade | Não | Sem ícone (já correto) | — |
| Resultado da Velocidade | Não | Sem ícone (já correto) | — |
| Termos de Uso | Não | Sem ícone (já correto) | — |

Resultado líquido: **2 telas ganham ícone** (Velocidade idle, Ferramentas), **1 perde** (Dispositivos),
**3 mantêm** (Início, Sinal, Histórico). As outras 10 já estavam corretas.

Padrão visual do ícone (já usado nas 3 telas que mantêm, replicar exatamente): `Icon` dentro de
`Row(verticalAlignment = Alignment.CenterVertically)` junto ao `Text` do título, `size = 18.dp`,
`tint = c.textPrimary`, `contentDescription = null` (decorativo — o texto do título já é o
rótulo acessível), `Spacer(Modifier.width(LkSpacing.xs))` entre ícone e texto.

**Nota sobre a spec do Claude Design ("SignallQ App - Fluxo de Telas"):** não consegui abrir o
artefato Claude Design diretamente nesta sessão (sem acesso à ferramenta de design neste ambiente).
A decisão acima usa como evidência (a) `chrome.jsx` — que já confirma o ícone como escolha por tela,
não "todas ou nenhuma" — e (b) `docs_ai/_archive/2026-07-13_AUDITORIA_DESIGN_TOBE.md`, que valida
Sinal com ✅ (ícone bate com a spec) e classifica o ícone de Início como "TopBar customizada mais
rica que a spec (não é regressão, é extensão de produto)" — ou seja, mesmo a spec original não exige
remover ícone de Início. Nenhuma evidência disponível contradiz a regra "ícone = raiz de aba".
Se o Camilo ou a Claudete tiverem acesso à sessão Claude Design real, vale uma conferência rápida
antes de implementar — mas a regra acima já é suficientemente fechada e justificada para seguir.

---

## Decisão 2 — Comportamento de scroll da TopBar

**Regra fechada: TopBar estática em posição e em opacidade nas 16 telas — nenhuma reação a
scroll.** Elimina os 3 mecanismos coexistindo hoje: `rememberTopBarAlpha()` (fade custom,
`ui/component/TopBarAlpha.kt`, usado em Home/Histórico/Laudo/ResultadoVelocidade) e
`TopAppBarDefaults.enterAlwaysScrollBehavior()` (`SpeedTestScreen.kt:111`, único caso).

**Por quê estática, e não "aplicar a API oficial do M3 uniformemente" (a alternativa que a task
sugeria como preferível por padrão):**

1. **O spec é silencioso de propósito, não por omissão.** O único comportamento de scroll
   documentado no design system inteiro (`HANDOFF_README.md`) é a bottom nav sumir no scroll-down
   — "Nav inferior some no scroll-down e durante teste em andamento." Isso é vocabulário de
   movimento deliberado do app: **o rodapé reage, o topo é âncora fixa.** Fazer a TopBar também se
   mover ou desaparecer duplica esse sinal e briga por atenção com a bottom nav.
2. **O que `rememberTopBarAlpha()` faz não é "elevação sutil ao rolar" (que eu consideraria manter
   via `pinnedScrollBehavior()`) — é a barra inteira desaparecendo em opacidade** (`FADE_THRESHOLD_PX
   = 300`, `alpha` chega a `0f`). Isso é mais parecido com `enterAlwaysScrollBehavior` (que esconde)
   do que com um efeito de elevação. Ou seja: hoje já existem só 2 famílias de comportamento na
   prática (nenhum vs. "a barra some de alguma forma"), não 3 comportamentos genuinamente distintos
   — e a família "some" nunca foi decisão de design documentada, foi reinvenção pontual.
3. **11 das 16 telas já são estáticas.** Uniformizar para estático move 5 telas; uniformizar para
   uma API de scroll moveria 11. Estática é a mudança de menor risco e já é o comportamento
   majoritário hoje.
4. **Alinhamento com o North Star do produto** (`DESIGN.md`, "The Calm Translator"): "Movimento
   contido e funcional... sem bounce nem floreio." Uma TopBar que aparece/desaparece ou perde
   opacidade durante a leitura de métricas técnicas (Laudo, Resultado, Histórico — exatamente as
   telas afetadas) é o tipo de floreio que essa diretriz já rejeita.
5. **Higiene:** `TopBarAlpha.kt` é código próprio para resolver um problema que a API padrão do M3
   resolveria — mas neste caso a decisão de produto é não resolver o problema (sem efeito de
   scroll), então o componente vira código morto a remover, não a substituir por outra API.

### O que muda por tela

| Tela | Hoje | Depois |
|---|---|---|
| Home | `rememberTopBarAlpha()` aplicado em `Modifier.graphicsLayer { alpha = ... }` | Remove o modifier, TopBar sempre `alpha = 1f` |
| Histórico | idem | idem |
| Laudo | idem | idem |
| Resultado da Velocidade | idem | idem |
| Velocidade (idle, `SpeedTestScreen.kt`) | `TopAppBarDefaults.enterAlwaysScrollBehavior()` + `Modifier.nestedScroll(...)` + `scrollBehavior` no `CenterAlignedTopAppBar` | Remove os três — `CenterAlignedTopAppBar` sem `scrollBehavior`, `Scaffold` sem `nestedScroll` |
| Demais 11 telas | Nenhum comportamento | Sem mudança |

**Arquivo a remover por completo:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/TopBarAlpha.kt`
(as duas extensions `LazyListState.rememberTopBarAlpha()` / `ScrollState.rememberTopBarAlpha()`,
depois de remover as 4 chamadas). Confirmar com `grep -r rememberTopBarAlpha` que não sobra
nenhum uso antes de apagar o arquivo — parte da validação obrigatória de higiene ao remover código.

---

## Decisão 3 — Botão de Ajustes (bônus da issue)

**Não é intencional na forma atual — é inconsistência a corrigir, mas a causa raiz não é "Ajustes
deveria ter botão de navegação como aba principal".** Ajustes **não é mais aba principal** (ver
achado da Decisão 1) — é overlay alcançado pelo avatar (`Overlay.Perfil`, GH#936). Como overlay
modal (não como item de pilha de navegação forward), o ícone correto de dispensa é mesmo o **X**
("Fechar"), não uma seta de voltar — isso está certo hoje.

**O que está errado é só a posição:** `AjustesScreen.kt:176-186` coloca o `IconButton` de "Fechar"
dentro de `actions` (lado direito), quando toda outra tela secundária do app coloca o controle de
dispensa (seta ou, aqui, X) em `navigationIcon` (lado esquerdo) — é onde o usuário aprendeu a
procurar "como eu saio daqui". Hoje o lado direito de Ajustes fica vazio (nenhum `actions`
contextual real), então mover o botão não colide com nada.

**Decisão: mover o `IconButton` de `actions` para `navigationIcon`, mantendo o ícone `Icons.Filled.Close`
e o comportamento (`onVoltar`) exatamente como estão.** Troca de posição apenas — sem mudança de
ícone, de lógica, ou de contrato (`onVoltar: (() -> Unit)? = null` continua controlando se o botão
aparece).

---

## Resumo para o Camilo — o que implementar, sem ambiguidade

1. **Ícone no título** — adicionar em Velocidade (idle) `Icons.Outlined.Speed` e em Ferramentas
   `Icons.Outlined.Build`, seguindo exatamente o padrão visual já usado em Início/Sinal/Histórico
   (`Row` + `Icon size=18.dp tint=c.textPrimary contentDescription=null` + `Spacer(LkSpacing.xs)`
   antes do `Text`). Remover o `Icon` do título em `DispositivosScreen.kt` (linhas ~138-143),
   mantendo só o `Text`.
2. **Scroll** — remover as 4 chamadas de `rememberTopBarAlpha()` (Home, Histórico, Laudo,
   ResultadoVelocidade) e o `Modifier.graphicsLayer { alpha = topBarAlpha }` associado a cada uma;
   remover `enterAlwaysScrollBehavior()` + `nestedScroll` + `scrollBehavior` em `SpeedTestScreen.kt`.
   Apagar `ui/component/TopBarAlpha.kt` depois de confirmar (grep) que não sobrou uso.
3. **Ajustes** — mover o `IconButton`/`Icon(Icons.Filled.Close, "Fechar")` de dentro de `actions`
   para dentro de `navigationIcon` em `AjustesScreen.kt`, sem alterar `onVoltar` nem o ícone.
4. Rodar `ktlintCheck` / `detekt` / `test` / `assembleDebug` nos módulos tocados (regra de
   higiene, seção 12) antes de abrir PR.

## Nota separada — divergência de documentação a registrar

`CLAUDE.md` (seção Identidade) ainda descreve "5 abas (Inicio, Velocidade, Sinal, Historico,
Ajustes)". A topologia real do bottom nav desde GH#936 é **Início, Velocidade, Sinal, Histórico,
Ferramentas** (Ajustes virou overlay via avatar). Isso é divergência de documentação (seção 4.10
da regra de higiene), não corrigida neste documento — reportando para Claudete/Rhodolfo
atualizarem `CLAUDE.md` numa tarefa própria, já que toca em mais de uma seção do arquivo.
