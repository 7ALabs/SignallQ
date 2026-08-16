---
title: "Ponto de extensão de overlays do AppShell"
description: "Como plugar overlay/rota novo no AppShell.kt sem editar o arquivo central — padrão criado pela issue #1695 para as fatias restantes do épico #1647."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-16"
version: "1.0.0"
---

# Ponto de extensão de overlays do AppShell

- **Status:** ativo
- **Última validação:** 2026-08-16 (issue #1695, épico #1647)
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

1. **Adicionar o valor em `AppShellOverlay`** (`AppShellNavigation.kt`). Continua sendo o único
   lugar que declara quais overlays existem.
2. **Criar `AppShellXxxOverlay.kt`** com um `@Composable internal fun` que recebe *só o que
   precisa* — nunca a lista inteira de parâmetros do `AppShell`. Ver `AppShellAssistOverlay.kt`
   (issue #1656) ou qualquer um dos seis migrados nesta issue
   (`AppShellTermosOverlay.kt`, `AppShellNovidadesOverlay.kt`, `AppShellPrivacidadeOverlay.kt`,
   `AppShellDetalhesTecnicosOverlay.kt`, `AppShellSinalWifiOverlay.kt`, `AppShellPingOverlay.kt`)
   como referência de tamanho/forma (20-45 linhas cada). O arquivo:
   - recebe `overlayStack: MutableList<AppShellOverlay>` e faz o próprio
     `AnimatedVisibility`/`zIndex` (via `rememberOverlayZIndex`, de `AppShellAssistOverlay.kt`) —
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
4. **Se o overlay precisar de um dado que `AppShell.kt` já expõe** (os grupos `AppShellXxxState`
   de `AppShellState.kt`, `overlayStack`, `navigator`, `localDevice` etc.), repasse-o como
   parâmetro novo de `AppShellOverlayRegistry` — e no único call site em `AppShell.kt`. Essa é a
   única linha que muda em `AppShell.kt` quando o dado ainda não circulava por ali; se o overlay
   só precisa do que já está exposto, `AppShell.kt` não muda nada.

### O que fica de fora por enquanto

Sheets sem back-stack (`showMonitoramentoSheet`, `showEquipamentoCredenciaisSheet`,
`showGerenciarDadosSheet`, `showAjudaSuporteSheet`, `showSobreAppSheet` — todas em `AppShell.kt`)
não usam `AppShellOverlay`/`overlayStack`, então não se encaixam neste registro por enquanto.
Migrá-las (se fizer sentido dar a elas o mesmo tratamento de rota) é escopo de uma issue futura,
não desta.

## Estado migrado nesta issue (prova de conceito)

7 overlays migrados de `AppShell.kt` para `AppShellOverlayRegistry.kt`: `Assist` (rewire do que a
#1656 já tinha extraído), `Termos`, `Novidades`, `Privacidade`, `DetalhesTecnicos`, `SinalWifi` e
`Ping`. `AppShell.kt` caiu de 1703 para 1646 linhas.

Overlays que **continuam inline** em `AppShell.kt` — migração é responsabilidade de quem tocar
cada área numa fatia futura, não obrigação retroativa desta issue:

- `Ajustes`, `Perfil`, `Ferramentas` (hubs com muitos callbacks cruzados)
- `Dispositivos`, `Fibra`/`EquipamentoInternet` (issues #1663/#1664 do épico)
- `Laudo`, `Dns`, `SinalCanais` (issues #1660/#1661/#1665 do épico)
- `ResultadoVelocidade`, `DiagnosticoGuiado`, `ModoGamer` (issues #1658/#1659/#1667 do épico)

## Testes

`AppShellOverlayRegistryTest.kt` (`android/app/src/test/kotlin/io/signallq/app/ui/screen/`) cobre,
por overlay migrado: visibilidade condicionada à presença na pilha, `onVoltar` removendo só o
próprio overlay, e (para `DetalhesTecnicos`) a condição extra de não desenhar com resultado nulo
mesmo com o overlay empilhado. Um teste de composição cruzada confirma que dois overlays
independentes não interferem entre si (remover um não afeta o outro). Novo overlay migrado deve
seguir o mesmo padrão de teste antes de entrar no registro — é o teste de caracterização que
protege contra regressão silenciosa de pilha/z-index/estado.

## Referências

- Issue #1695 · Épico #1647 (`docs_ai` não versiona issues — ver GitHub)
- Regra de higiene, §4.3 (`.claude/rules/higiene-e-padronizacao-repositorio.md`)
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellNavigation.kt` (pilha/push/pop)
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellOverlayRegistry.kt` (KDoc com o
  mesmo passo a passo, para quem só tem o código aberto)
