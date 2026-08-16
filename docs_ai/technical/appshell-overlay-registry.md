---
title: "Ponto de extensão de overlays do AppShell"
description: "Como plugar overlay novo no AppShell.kt quase sempre sem editar o arquivo central — padrão criado pela issue #1695 para as fatias restantes do épico #1647. Não cobre rota (navegação entre raízes)."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-16"
version: "1.1.0"
---

# Ponto de extensão de overlays do AppShell

- **Status:** ativo
- **Última validação:** 2026-08-16 (issue #1695, épico #1647 — correções pós-revisão de Caio na
  PR #1697: números reais de onde vieram as linhas, remoção da promessa indevida sobre rota,
  migração do `Dns`)
- **Fonte de verdade:** este documento para o padrão; o código
  (`android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellOverlayRegistry.kt`) é a fonte
  de verdade do comportamento real — se divergirem, o código vence (regra de higiene §3).
- **Escopo:** módulo `:app`, telas overlay empilhadas pelo `AppShell.kt` (Jornada 2.0, épico
  #1647). Não cobre a navegação entre as 4-5 raízes (tabs) nem o mecanismo de push/pop em si
  (isso é `AppShellNavigation.kt`, inalterado por esta issue).
- **Responsável:** Camilo (cria e mantém), Caio revisa mudanças de arquitetura.

## Objetivo

`AppShell.kt` é o shell de composição e navegação do Consumer — toda tela overlay (Perfil, Sinal
Wi-Fi, Detalhes técnicos, etc.) historicamente precisava de um bloco `AnimatedVisibility` inline
ali para aparecer/desaparecer da pilha. Como o épico #1647 adiciona uma fatia vertical nova a cada
issue, isso concentrou risco e travou paralelismo (ver regra de serialização do épico) — duas
fatias que tocam áreas diferentes não podiam rodar ao mesmo tempo por causa da fiação
compartilhada num único arquivo de 1700+ linhas.

## O que não resolveu antes (contexto da issue #1695)

Entre as fatias 2.0.03 e 2.0.08, `AppShell.kt` saiu de 1670 linhas, caiu para 1518 numa extração
real (2.0.03, que criou `AppShellNavigation.kt`), e voltou a subir em **5 fatias consecutivas**
até 1703 — acima de onde tinha começado. Cada fatia cumpriu a letra da regra de extração da
higiene (§4.3/§7: "extraia ao menos uma responsabilidade relacionada"), inclusive a 2.0.08 que
criou `AppShellAssistOverlay.kt` (91 linhas) — mas devolveu ~40 linhas de fiação de volta para
`AppShell.kt`. Extrair um overlay não bastava: **o call site do overlay novo continuava sendo
adicionado dentro do arquivo central**, então o arquivo nunca parava de crescer.

## O que este registro não resolve (medido, não estimado)

A revisão de Caio na PR #1697 mediu de onde vieram, linha a linha, as ~226 linhas que as 5 fatias
acima devolveram a `AppShell.kt`:

| Fatia | Saldo | O que foi adicionado |
|---|---|---|
| 2.0.04 Perfil | +44 | bloco de overlay `Ajustes` + wiring + helper `abrirEmailSuporte` |
| 2.0.05 Ferramentas | +67 | lambda `disponibilidadeFerramenta` (~30 linhas de regra de negócio) + 1 bloco de overlay |
| 2.0.06 Início | +62/−43 | **100% wiring de root content** (`Inicio2Screen`/`HomeScreen`) — zero overlay |
| 2.0.07 Trilha | +13 | **100% wiring** (`connectionTrail`, `onAbrirTrailRoute`) — zero overlay |
| 2.0.08 Assist | +40 | estado hoisted (`assistObjetivoPreSelecionado` + 4 resets) + call site |

Blocos `AnimatedVisibility` de overlay — a única coisa que este registro cobre — respondem por
**~30-40 das ~226 linhas (~15%)**. As fatias 2.0.06 e 2.0.07 não adicionaram overlay nenhum: foi
100% wiring de root content. A 2.0.05 foi majoritariamente lambda de regra de negócio; a 2.0.08,
estado hoisted. Nada disso passa por `AppShellOverlayRegistry` — continua sendo risco de
crescimento do arquivo e não é escopo desta issue.

O ponto de extensão criado aqui é útil e cobre o problema original (overlay novo não precisa mais
de bloco `AnimatedVisibility` inline), mas é uma fração do que faz `AppShell.kt` crescer. Se o
objetivo é conter o crescimento por completo, o próximo alvo é **root content e estado hoisted**,
não mais overlays — é de lá que vieram os outros ~85%. Ver
[#1698](https://github.com/buildea-labs/signallq/issues/1698), aberta a partir do achado da
revisão da PR #1697 e sequenciada antes da Task 2.0.09 (#1657).

## O padrão

Duas responsabilidades, dois arquivos, sem sobreposição:

| Arquivo | Responsabilidade |
|---|---|
| `AppShellNavigation.kt` | **Quando** um overlay existe: o enum `AppShellOverlay`, a pilha por raiz (`AppShellNavigator`, push/pop/back/restauração de estado). Única fonte de verdade da navegação. |
| `AppShellOverlayRegistry.kt` | **O quê** desenha cada overlay: agrega os Composables de `AppShellXxxOverlay.kt` num único ponto de chamada. Não decide push/pop, só renderização. |

`AppShellOverlayRegistry.kt` não compete com `AppShellNavigation.kt` — são preocupações
ortogonais (a mesma distinção entre *routing* e *rendering* que já existia implicitamente antes,
só que sem um lugar dedicado para a segunda metade).

### Passo a passo para plugar um overlay novo

Cobre apenas overlays empilhados via `AppShellOverlay`/`overlayStack` — **não cobre rota**: a
navegação entre as raízes (tabs) segue com `when`/`Screen(` inline em `AppShell.kt` (19 call sites
de `Screen(` hoje), fora do escopo deste registro.

1. **Adicionar o valor em `AppShellOverlay`** (`AppShellNavigation.kt`). Continua sendo o único
   lugar que declara quais overlays existem.
2. **Criar `AppShellXxxOverlay.kt`** com um `@Composable internal fun` que recebe *só o que
   precisa* — nunca a lista inteira de parâmetros do `AppShell`. Ver `AppShellAssistOverlay.kt`
   (issue #1656) ou qualquer um dos sete migrados nesta issue
   (`AppShellTermosOverlay.kt`, `AppShellNovidadesOverlay.kt`, `AppShellPrivacidadeOverlay.kt`,
   `AppShellDetalhesTecnicosOverlay.kt`, `AppShellSinalWifiOverlay.kt`, `AppShellPingOverlay.kt`,
   `AppShellDnsOverlay.kt`) como referência de tamanho/forma (20-45 linhas cada). O arquivo:
   - recebe `overlayStack: MutableList<AppShellOverlay>` e faz o próprio
     `AnimatedVisibility`/`zIndex` (via `rememberOverlayZIndex`, definida em `AppShell.kt`) —
     exceto quando o overlay já tem sua própria animação de entrada/saída (ex.: `PingScreen`, uma
     `ModalBottomSheet`), caso em que só cuida do `zIndex`, sem duplicar transição
     (`AppShellPingOverlay.kt` é a referência desse caso);
   - `onVoltar`/equivalente remove **só o próprio overlay** da pilha
     (`overlayStack.remove(AppShellOverlay.Xxx)`);
   - callbacks que precisam tocar estado que sobrevive à recriação do overlay (ex.: pré-seleção do
     Assist) sobem como parâmetro de função, nunca como estado global novo.
3. **Registrar a chamada em `AppShellOverlayRegistry`** — adicionar a invocação do novo
   `AppShellXxxOverlay(...)` dentro da função `AppShellOverlayRegistry`. A ordem de declaração
   **não** decide o z-index visual (isso é `rememberOverlayZIndex`, baseado na posição real do
   overlay em `overlayStack`) — a entrada nova pode ir em qualquer lugar da lista.
4. **Se o overlay precisar de um dado que `AppShell.kt` ainda não expõe** (não está em nenhum dos
   grupos `AppShellXxxState` de `AppShellState.kt`, `overlayStack`, `navigator`, `localDevice`
   etc.), esse dado precisa circular por `AppShell.kt` primeiro: repasse-o como parâmetro novo de
   `AppShellOverlayRegistry` e no único call site em `AppShell.kt`. Nesse caso o arquivo central
   muda uma linha; se o overlay só precisa do que já está exposto, `AppShell.kt` não muda nada.
   Isto NÃO é uma exceção rara: por exemplo, um redesenho do `SinalWifi` que precisasse de um
   callback de entrada novo (ex.: vindo de uma recomendação) passaria por este passo 4.

### O que fica de fora por enquanto

Sheets sem back-stack (`showMonitoramentoSheet`, `showEquipamentoCredenciaisSheet`,
`showGerenciarDadosSheet`, `showAjudaSuporteSheet`, `showSobreAppSheet` — todas em `AppShell.kt`)
não usam `AppShellOverlay`/`overlayStack`, então não se encaixam neste registro por enquanto.
Migrá-las (se fizer sentido dar a elas o mesmo tratamento) é escopo de uma issue futura, não desta.

## Estado migrado nesta issue

8 overlays migrados de `AppShell.kt` para `AppShellOverlayRegistry.kt`: `Assist` (rewire do que a
#1656 já tinha extraído), `Termos`, `Novidades`, `Privacidade`, `DetalhesTecnicos`, `SinalWifi`,
`Ping` e `Dns` (GH#933 Fase 4 — 14 linhas, 4 parâmetros, mais simples que o `DetalhesTecnicos` já
migrado; adicionado após o parecer de Caio na PR #1697 apontar que não havia critério de risco
para deixá-lo de fora, e a issue #1665 do épico vai precisar dele). `AppShell.kt` caiu de 1703
para 1635 linhas.

Overlays que **continuam inline** em `AppShell.kt` — migração é responsabilidade de quem tocar
cada área numa fatia futura, não obrigação retroativa desta issue:

- `Ajustes`, `Perfil`, `Ferramentas` (hubs com muitos callbacks cruzados — `Ajustes` sozinho tem
  59 atribuições de parâmetro em 93 linhas, adiar está justificado por volume, não por preguiça)
- `Dispositivos`, `Fibra`/`EquipamentoInternet` (issues #1663/#1664 do épico)
- `Laudo`, `SinalCanais` (issues #1660/#1661 do épico)
- `ResultadoVelocidade`, `DiagnosticoGuiado`, `ModoGamer` (issues #1658/#1659/#1667 do épico)

## Testes

`AppShellOverlayRegistryTest.kt` (`android/app/src/test/kotlin/io/signallq/app/ui/screen/`) cobre
cada uma das 8 entradas em duas camadas: por overlay (`AppShellXxxOverlay` chamado direto —
visibilidade condicionada à presença na pilha, `onVoltar` removendo só o próprio overlay) e por
registro (`AppShellOverlayRegistry` chamado inteiro — confirma que a chamada para aquele overlay
existe dentro do agregador). A segunda camada existe porque a primeira PR desta issue não a tinha:
a revisão de Caio (PR #1697) rodou mutação e mostrou que remover a chamada de `SinalWifi`, `Ping`,
`DetalhesTecnicos` ou `Privacidade` de dentro de `AppShellOverlayRegistry` deixava a suíte verde —
a tela simplesmente sumia do app sem nenhum teste vermelho. O teste de `DetalhesTecnicos` também
foi corrigido: a asserção original passava igual com a guarda `&& resultadoSpeedtest != null`
removida (o `?.let` interno já omitia o texto sozinho); agora usa um `testTag` no container do
`AnimatedVisibility` para distinguir "não compôs" de "compôs vazio". Um teste de composição
cruzada confirma que dois overlays independentes não interferem entre si (remover um não afeta o
outro). Novo overlay migrado deve seguir as duas camadas antes de entrar no registro.

## Referências

- Issue #1695 · Épico #1647 (`docs_ai` não versiona issues — ver GitHub)
- Regra de higiene, §4.3 (`.claude/rules/higiene-e-padronizacao-repositorio.md`)
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellNavigation.kt` (pilha/push/pop)
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellOverlayRegistry.kt` (KDoc com o
  mesmo passo a passo, para quem só tem o código aberto)
