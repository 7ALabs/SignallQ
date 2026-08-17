---
title: "Ponto de extensão de root content do AppShell"
description: "Como migrar/plugar uma raiz (tab) do AppShell.kt sem inchar o arquivo central — padrão criado pela issue #1698 para as fatias restantes do épico #1647. Irmão do registro de overlays, cobre o vetor de ~85% que aquele não cobria."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-16"
version: "1.0.0"
---

# Ponto de extensão de root content do AppShell

- **Status:** ativo
- **Última validação:** 2026-08-16 (issue #1698, épico #1647 — 2 das 5 raízes migradas)
- **Fonte de verdade:** este documento para o padrão; o código
  (`android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellRootRegistry.kt`) é a fonte de
  verdade do comportamento real — se divergirem, o código vence (regra de higiene §3)
- **Escopo:** módulo `:app`, as 5 raízes (tabs) do `AppShell.kt`. Não cobre overlay (isso é
  [`appshell-overlay-registry.md`](appshell-overlay-registry.md)) nem a navegação em si
  (`AppShellNavigation.kt`, inalterado)
- **Responsável:** Camilo (cria e mantém), Caio revisa mudanças de arquitetura
- **Documentos relacionados:** [`appshell-overlay-registry.md`](appshell-overlay-registry.md)
  (irmão, criado pela #1695)

## Por que existe

A #1695 criou o registro de **overlays** partindo da hipótese de que overlay era o vetor dominante
de crescimento do `AppShell.kt`. Na revisão da PR #1697, Caio mediu de onde vieram, linha a linha,
as ~226 linhas que as 5 fatias 2.0.04–2.0.08 devolveram ao arquivo:

| Fatia | Saldo | O que foi adicionado |
|---|---|---|
| 2.0.04 Perfil | +44 | bloco de overlay `Ajustes` + wiring + helper |
| 2.0.05 Ferramentas | +67 | lambda `disponibilidadeFerramenta` (~30 linhas de regra) + 1 overlay |
| 2.0.06 Início | +62/−43 | **100% wiring de root content** — zero overlay |
| 2.0.07 Trilha | +13 | **100% wiring de root content** — zero overlay |
| 2.0.08 Assist | +40 | estado hoisted + call site |

Overlay responde por **~15%**. Root content, estado hoisted e lambdas de regra de negócio
respondem pelos outros **~85%** — e nada disso passava pelo registro de overlays. Daí esta issue.

## O padrão

Três responsabilidades, três arquivos, sem sobreposição:

| Arquivo | Responsabilidade |
|---|---|
| `AppShellNavigation.kt` | **Quando/qual**: enum `AppShellRoot`, `AppShellNavigator`, pilha por raiz, back. Fonte de verdade única da navegação. |
| `AppShellRootRegistry.kt` | **O quê desenha cada raiz**: agrega os Composables de `AppShellXxxRoot.kt`. Não decide seleção de raiz nem back. |
| `AppShellOverlayRegistry.kt` | O mesmo, para overlay empilhado. |

Não há segundo motor de navegação: o registro recebe a raiz já resolvida
(`navigator.selectedRoot`) e só decide qual Composable desenhar.

### Regra central — um parâmetro por raiz, nunca N campos soltos

Esta é a decisão que a #1698 exigia **antes** da primeira migração, herdada da ressalva 3 de Caio
na PR #1697: `AppShellOverlayRegistry` chegou a **17 parâmetros** e ganhava ~4 por overlay migrado,
com 10 overlays ainda por migrar — na trajetória atual, o registro viraria ele mesmo o próximo
ponto de concentração, trocando um monolito por outro.

No registro de raízes, cada raiz contribui com **exatamente um** parâmetro (um `@Stable data class`
`AppShellXxxRootEntry`). Migrar as 3 raízes restantes leva a assinatura de 4 para 6 parâmetros, não
para 40.

**Isto não é opcional e o linter não protege:** `detekt` tem a regra `LongParameterList` com
threshold 8, mas `build.maxIssues: 2000` no config torna o gate mudo. O limite é ultrapassado em
silêncio — é dívida pré-existente do repositório (não desta issue), mas significa que a disciplina
aqui é humana, não automatizada.

### Passo a passo para migrar uma raiz

1. **Criar `AppShellXxxRoot.kt`** com um `@Composable internal fun` que recebe só o que aquela raiz
   precisa. Quando forem mais que ~3 campos, declare também um `@Stable data class` de grupo no
   mesmo arquivo (`AppShellHistoricoState`, `AppShellFerramentasAcoes` são as referências).
2. **Mover a construção do grupo para a `MainActivity`**, que já monta `AppShellSpeedtestState`,
   `AppShellWifiState` etc. desde antes deste épico. Este passo é o que **de fato encolhe**
   `AppShell.kt` — ver "Saldo real" abaixo. Só funciona quando o dado é estado de ViewModel; quando
   o grupo é feito de callbacks que empilham overlay (caso do hub Ferramentas), a construção é
   inerentemente do shell e fica onde está.
3. **Mover a entrada** de `naoMigradas` para o `when` de `AppShellRootRegistry`, criando o
   `AppShellXxxRootEntry` correspondente.
4. **Escrever as três camadas de teste** (ver abaixo) antes de considerar pronto.

### O slot `naoMigradas`

Escapatória temporária que recebe as raízes ainda inline em `AppShell.kt`. Hoje: `Home`, `Speed`,
`Wifi`. **Encolhe a cada fatia** — quando a última migrar, o parâmetro sai junto com o slot. Raiz
nova nasce migrada; o slot não é destino de nada novo.

## Estado migrado nesta issue

| Raiz | Arquivo | O que veio junto |
|---|---|---|
| `History` (tab 3) | `AppShellHistoricoRoot.kt` | 7 parâmetros soltos do `AppShell` viraram `AppShellHistoricoState`, construído na `MainActivity` |
| `Tools` (tab 4) | `AppShellFerramentasRoot.kt` | a regra `resolverDisponibilidadeFerramenta` (~28 linhas, agora função pura) e os 9 callbacks em `AppShellFerramentasAcoes` |

Ainda inline (via slot): `Home` (tem o par `Inicio2Screen`/`HomeScreen` sob `shellMode`), `Speed`,
`Wifi`. Migrar cada uma é responsabilidade de quem tocar aquela área numa fatia futura.

## Saldo real — e por que ele é menor do que parece

**`AppShell.kt`: 1641 → 1635 linhas (−6).** A tabela de saldo que o épico passou a exigir precisa
mostrar isso sem maquiagem:

| Movimento | Efeito em `AppShell.kt` |
|---|---|
| regra `disponibilidadeFerramenta` extraída | −28 |
| call sites de `HistoricoScreen` e `FerramentasScreen` (tab) | −31 |
| 7 parâmetros do Histórico → 1 grupo | −7 |
| ternário `onAbrirMenu` repetido 4× → 1 alias | −3 |
| **construção das 2 `RootEntry` + grupo de ações + comentários** | **+63** |

> Correção após revisão (Caio, PR #1702): uma versão anterior desta tabela creditava −4 ao
> `Overlay.Ferramentas` por "desduplicar 9 atribuições". **Não procede** — aquele call site foi de
> 9 linhas para 9 linhas; o grupo trocou `onAbrirSinalCanaisOverlay` por
> `acoesFerramentas.onAbrirSinalCanais`, o que é mais indireto sem remover nada. O ganho ali é de
> manutenção (uma fonte só para as duas superfícies), não de linhas.

**Conclusão que importa para as próximas fatias:** re-embrulhar campos soltos em grupo construído
*dentro* do shell não encolhe o arquivo — só troca a forma do wiring. O que encolhe é o **passo 2**
(mover a construção para a `MainActivity`), e ele só se aplica quando o grupo é estado, não
callback de navegação. Histórico conseguiu; Ferramentas não, porque seus 9 callbacks empilham
overlay no `overlayStack` do shell.

O ganho real desta fatia, portanto, **não é a contagem de linhas de hoje** — é o custo marginal da
próxima fatia: mexer em filtro/resumo/lista do Histórico agora edita `AppShellHistoricoRoot.kt`;
mexer na regra de disponibilidade do hub edita `AppShellFerramentasRoot.kt` (com teste unitário
barato, sem compor o shell). Nenhuma das duas toca o arquivo central.

## Testes

`AppShellRootRegistryTest.kt` e `ResolverDisponibilidadeFerramentaTest.kt`
(`android/app/src/test/kotlin/io/signallq/app/ui/screen/`), em três camadas:

1. **por raiz** — `AppShellXxxRoot` chamado direto: repasse de estado, callbacks e regra;
2. **por registro** — `AppShellRootRegistry` inteiro: confirma que a entrada daquela raiz existe
   dentro do agregador;
3. **pelo slot** — `naoMigradas` é chamado para as 3 raízes inline e **nunca** para as 2 migradas
   (camada que não existe no registro de overlays; pega a reversão silenciosa de uma migração).

A camada 2 existe porque a revisão da PR #1697 provou, por mutação, que sem ela apagar a chamada de
um overlay do registro deixava a suíte verde e a tela sumia do app.

### Mutação executada (2026-08-16/17)

| Mutante | Resultado |
|---|---|
| entrada `History` removida do registro | **morto** por 3 testes |
| entrada `Tools` removida do registro | **morto** por 3 testes |
| `DISPOSITIVOS` lendo `wifiEnabled` em vez de `devicesEnabled` | **morto** por 1 teste |
| `onAbrirPing` recebendo `acoes.onAbrirDns` (swap de campos) | **morto** |
| `onIniciarTeste` → `{}` | **morto** |
| `onAbrirMenu` (Histórico) → `{}` | **morto** (teste acrescentado após revisão) |
| `onAbrirMenu` (Ferramentas) → `{}` | **morto** (idem) |
| `onRegistrarAbertura` → `{}` | **morto** (idem) |
| `adsEnabled` → `false` | **sobrevive** — ver abaixo |

**O escopo exato da cobertura, para não sobre-declarar:** as duas *entradas* do registro estão
100% cobertas — remover `History` ou `Tools` do `when` mata a suíte, que é o critério de aceite
literal da issue. No nível de *campo* das `RootEntry`, 7 de 8 estão cobertos. As três últimas
linhas da tabela foram acrescentadas depois da revisão de Caio na PR #1702, que rodou 12 mutantes
e encontrou 4 sobreviventes — os helpers do teste fixavam `onAbrirMenu = {}`, `adsEnabled = false`
e `onRegistrarAbertura = {}` sem nunca variar, então não havia valor distinguível.

**`adsEnabled` continua descoberto, e é limite de ambiente, não descuido.** `rememberNativeAd` só
devolve anúncio não nulo em `NativeAdLoadState.Fill`, que exige resposta real do AdMob, e
`NativeAdCard` faz `if (nativeAd == null) return` na primeira linha. Sob Robolectric não há fill,
então `eligible = true` e `eligible = false` renderizam a mesma árvore vazia — nenhuma asserção de
UI os distingue. Cobrir exigiria injetar um `NativeAdRequester` fake até a raiz, mudando o contrato
de `AppShellHistoricoRoot` só para viabilizar teste; isso é decisão da arquitetura de ads
(#1330/#1694), não desta fatia. Até lá, o campo depende de revisão humana do diff.

Raiz nova migrada deve repetir as três camadas **e** exercitar cada campo da sua `RootEntry` com
valor distinguível — foi exatamente essa a lacuna encontrada aqui.

## Referências

- Issue #1698 · #1695 · #1697 · Épico #1647 (`docs_ai` não versiona issues — ver GitHub)
- [`appshell-overlay-registry.md`](appshell-overlay-registry.md) — registro irmão, para overlay
- Regra de higiene, §4.3 (`.claude/rules/higiene-e-padronizacao-repositorio.md`)
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellNavigation.kt` (raiz/pilha/back)
