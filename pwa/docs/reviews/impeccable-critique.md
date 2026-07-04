# Critique — Principais telas do webapp (Landing, Home, SpeedTest, Result, History, Settings)

> Gerado via `/impeccable critique`, 2026-07-04. Foco pedido: clareza, hierarquia visual, excesso de cards, textos confusos, aparência genérica de IA. Método: dual-agent (A: revisão de design · B: detector + evidência), ambos isolados. Snapshot bruto também salvo em `.impeccable/critique/2026-07-04T22-20-15Z__app-home-landing-speedtest-result-history-settings.md`.

## Design Health Score

| # | Heurística | Nota | Problema-chave |
|---|---|---|---|
| 1 | Visibilidade do status | 2 | `ProgressRing` fica travado em `value="--"` a run inteira (`SpeedTestScreen.tsx:95`) — nunca mostra número real durante o teste |
| 2 | Correspondência com o mundo real | 2 | Jargão "ICMP" no meio do teste (`SpeedTestScreen.tsx:100-102`) |
| 3 | Controle e liberdade do usuário | 3 | Cancelar presente e claro |
| 4 | Consistência e padrões | 2 | 3 vocabulários de veredito diferentes para o mesmo eixo bom/atenção/ruim |
| 5 | Prevenção de erros | 3 | Botão de limpar histórico desabilitado quando vazio |
| 6 | Reconhecimento > memorização | 3 | Ícone+texto emparelhados na maioria das ações |
| 7 | Flexibilidade e eficiência | 2 | Nenhum atalho, nenhuma ação em lote |
| 8 | Estética e minimalismo | 2 | Empilhamento de 5-7 caixas bordadas no Result/Detail |
| 9 | Recuperação de erros | 2.5 | Erro de histórico tratado; estado de erro do speedtest não avaliado |
| 10 | Ajuda e documentação | 3 | "Sobre" presente, mas raso |
| **Total** | | **24.5/40** | **Acceptable — melhorias significativas necessárias** |

## Veredito Anti-Patterns

**Parcial.** Nenhum clichê grosseiro (sem gradiente em texto, sem glassmorphism, sem stripe lateral, sem eyebrows numerados). Mas dois tells reais aparecem: **(1)** o grid de 4 métricas em caixas idênticas no Result/TestDetail é exatamente o "stat cards em grade" que a marca deveria evitar; **(2)** uso sistemático do `.overline` (eyebrow uppercase) em quase toda tela.

O scanner determinístico (`detect.mjs`) rodou sobre as 6 telas e achou só 2 advisories (radius 1px, cor `#000000` não documentada) — ambos prováveis falsos positivos (hairline divider, primitivo de sombra). O scanner não captura o problema real porque verifica drift de token, não excesso semântico de containers. A contagem manual confirma isso:

| Tela | Caixas bordadas |
|---|---|
| Landing / SpeedTest | 0 |
| Settings | 3 (bem agrupado, 1 por seção) |
| Home | até 3 |
| History (painel) | 0-1 |
| **Result / TestDetail** | **até 7 cada** |

Visualização em navegador indisponível nesta sessão (sem ferramenta de automação exposta) — evidência é 100% leitura de código-fonte + CSS.

## Impressão Geral

O sistema visual é disciplinado — nada dos clichês mais grosseiros de "feito por IA". O problema real é que a tela-clímax do produto (Result/TestDetail, onde a promessa "diagnóstico honesto" é entregue) é justamente a mais fragmentada em caixas, com 3 vocabulários de veredito coexistindo no código e jargão técnico surgindo no pico de ansiedade do usuário. A maior oportunidade não é "menos slop visual" — é consolidar o Result num cartão único e unificar a linguagem de veredito.

## O que Funciona

1. **`SpeedTestScreen`** — mais disciplinada do conjunto: `ProgressRing` + `StepTracker`, zero cards.
2. **`SettingsPanel`** — um único card por seção com divisores internos, o padrão certo.
3. Par "métrica crua + veredito humano" bem implementado em `ResultScreen.tsx:116-155` — cumpre a promessa central da marca.

## Priority Issues

**[P1] Grid de métricas vira "stat cards" genéricos**
- **Por quê**: 4 caixas idênticas (`sq-metric-block`) em `ResultScreen.tsx:115-156`/`TestDetailScreen.tsx:115-156` — o maior tell de "dashboard SaaS" do conjunto, confirmado pela contagem (até 7 caixas na tela).
- **Fix**: consolidar num único card com divisores internos (padrão já existe em `sq-home-screen__result-grid`).
- **Comando**: `/impeccable distill`

**[P1] Três vocabulários de veredito diferentes**
- **Por quê**: `statusTitle` ("boa/atenção/ruim"), `metricVerdict` ("Fraca/Regular/Boa"), `qualityLabel` ("Bom/Atenção/Ruim/Inconclusivo") — três escalas para o mesmo eixo, direto contra a promessa de clareza.
- **Fix**: consolidar num módulo único de vocabulário de veredito.
- **Comando**: `/impeccable clarify`

**[P2] Redundância na Home** — "Histórico" 3x na mesma tela; card vazio bordado para uma frase. → `/impeccable layout`

**[P2] Jargão ICMP no pico de ansiedade** (`SpeedTestScreen.tsx:100-102`) → `/impeccable clarify`

**[P2] Lógica de veredito duplicada byte-a-byte** entre Result e TestDetail — risco de divergência silenciosa futura. → `/impeccable distill`

## Persona Red Flags

**Jordan (primeira vez)**: pill da Landing ("servidor estimado") contradiz a pill da Home ("medição direta") em telas consecutivas; dois vocabulários de veredito na mesma jornada; jargão ICMP sem explicação.

**Casey (mobile, distraído)**: botão com ícone `bookmark_add` mas handler `onCopyLink` — rótulo/ícone/função não combinam; "Histórico" duplicado mesmo no mobile; Result empilha 5-7 blocos uniformes sem hierarquia de leitura.

## Minor Observations

- "teste(s) salvos" com parênteses lê como placeholder não localizado.
- "Versão do app 1.0.0 · web" hardcoded, provavelmente desatualizado.
- "Pede confirmação." em Settings é narração autorreferente sem valor.
- Classes `history-panel__message` sem prefixo `sq-`, quebrando convenção.
- `LimitationsCard` sempre âmbar mesmo em resultado bom — dilui o peak-end positivo.

## Questions to Consider

1. Se o Result é o clímax do produto, por que é a tela mais fragmentada em caixas do conjunto?
2. Três vocabulários de veredito — decisão deliberada ou nunca unificado?
3. Ring travado em "--" durante o teste inteiro — simplicidade deliberada ou sinal real perdido?

## Decisões do Luiz (Ask the User)

- Prioridade: **tudo de uma vez** (P1 e P2 juntos).
- Escopo: **incluir a Home** nesta rodada (redundância do "Histórico" 3x e card vazio), além de Result/TestDetail.

## Ações Recomendadas

1. **`/impeccable distill Result/TestDetailScreen`** — consolidar as 4 caixas de métrica num único card com divisores internos; extrair `verdictFromQuality`/`statusTitle`/`metricVerdict` para um módulo compartilhado, eliminando a duplicação byte-a-byte entre as duas telas.
2. **`/impeccable clarify Result/TestDetail/HistoryPanel`** — unificar os 3 vocabulários de veredito num só; remover o jargão ICMP do `SpeedTestScreen` e mover para Sobre/tooltip.
3. **`/impeccable layout HomeScreen`** — remover a redundância do "Histórico" (3x na mesma tela) e trocar o card vazio bordado por texto simples.
4. **`/impeccable polish`** — passe final depois das correções acima (inclui os minor observations: texto "teste(s)", versão hardcoded, classes sem prefixo `sq-`, `LimitationsCard` sempre âmbar).

Re-rode `/impeccable critique` depois das correções para ver o score subir.
