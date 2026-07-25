# SignallQ Site/PWA — reconstrução 1:1 (protótipo v2)

- **Status:** pronto para implementação — Fase 0 (fundação) a ser disparada
- **Origem:** protótipo Claude Design "SignallQ Web - Prototipo"
  (`https://claude.ai/design/p/8138b35c-210d-4e21-94ef-d48a3e18e94c`), lido via `DesignSync` pela
  Claudete (sessão principal — subagentes não têm acesso a essa tool, mesma limitação já registrada
  em `.claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md`). Arquivos baixados nesta pasta
  para a Lia trabalhar a partir de disco.
- **Decisão do Luiz (2026-07-25):** implementação deve bater **1:1** em conteúdo, layout e
  disposição — desktop e mobile, claro e escuro (por `prefers-color-scheme`, sem toggle manual,
  igual ao `useSystemTheme.ts` que já existe). A moldura de iPhone do protótipo (`ios-frame.jsx`,
  componente `IOSDevice`) é só ferramenta de apresentação do Claude Design — **não replicar**.
  A implementação real usa breakpoint responsivo de verdade (mesmo padrão `lg:` já usado no Site),
  cobrindo qualquer largura de monitor/celular, não uma largura fixa.

## Arquivos neste pacote

Cópias verbatim dos componentes `.dc.html` do protótipo (formato Claude Design — `sc-if`/`sc-for`/
`{{ }}` são a sintaxe do preview, não JSX/React; a lógica de `renderVals()` no `<script data-dc-script>`
de cada arquivo documenta as variantes e a matemática de cada tela, útil de referência mesmo que a
implementação real seja em React/TS).

| Arquivo | Rota real | Observação |
|---|---|---|
| `SiteNav.dc.html` | compartilhado | 5 itens: Teste, Histórico, Como funciona, Quem somos, Privacidade |
| `SiteFooter.dc.html` | compartilhado | 3 densidades (mobile/compact/full) |
| `AdRail.dc.html` | compartilhado | coluna de anúncio lateral, 240px, só desktop — variantes `a`/`b` |
| `AdBannerWide.dc.html` | compartilhado | banner horizontal, todas as larguras (compact em mobile) — variantes `a`/`b` |
| `ScreenHome.dc.html` | `/` | idle, running, result, result-parcial, 4 estados de erro, toasts PWA |
| `ScreenHistorico.dc.html` | `/historico` | lista (com gráfico de evolução novo), limpar, vazio, indisponível, carregando |
| `ScreenPro.dc.html` | `/pro` | parte 1 (proposta/entregas), parte 2 (fluxo/comparativo/planos), modal lista de espera |
| `ScreenDoc.dc.html` | `/como-medimos`, `/quem-somos`, `/privacidade`, `/termos`, `/internet-boa-mas-travando`, `/lag-em-jogos-online` | um componente, prop `page` troca conteúdo — copy completo de privacidade/termos já revisado (ver seção abaixo) |
| `Screen404.dc.html` | `*` | — |

Não incluídos (não fazem parte da implementação real, são só apresentação do protótipo):
`Desktop.dc.html`/`Webapp.dc.html` (galerias com moldura), `ios-frame.jsx`, `support.js`.

## Achado 1 — 3 espaços de anúncio local por tela, não 1

Todo `ScreenX` segue o mesmo layout: `SiteNav` → `AdRail` (esquerda, variant `a` implícito) +
conteúdo + `AdRail variant="b"` (direita) — ambos só desktop — → `AdBannerWide` (embaixo, todas as
larguras) → `SiteFooter`. A tela de Resultado (`ScreenHome`, variant `result`) tem um **4º** anúncio
embutido fixo dentro do card de resultado.

Isso expande a Feat #1402 (que tratava só do slot único de Resultado) para os 3 espaços universais.
Camilo (#1405-1407, PR #1409) e Lia (#1403-1404, PR #1410) já implementaram schema D1 + endpoint +
client de fallback (no-fill do AdSense → anúncio local sorteado) — esse mecanismo não muda; só a
**apresentação visual** (onde e quantos slots) muda quando esta reconstrução acontecer.

Conteúdo dos anúncios locais no protótipo: sempre promoção do próprio app SignallQ ("Entrar na
lista de teste", "Teste fechado", "App em desenvolvimento") — variantes de cor/copy `a` (roxo) e
`b` (azul).

## Achado 2 — rota nova

`/como-medimos` não existe no Site hoje. Confirmado com o Luiz (2026-07-25): criar como página nova
usando o `ScreenDoc` com `page="como-medimos"`.

## Achado 3 — copy legal pode ter mudado

O `ScreenDoc` traz texto completo e mais detalhado de Privacidade (11 seções, incluindo Cloudflare
Web Analytics, lista de espera do PRO, contato) e Termos (11 seções) do que pode estar em
`PrivacidadePage.tsx`/`TermosPage.tsx` hoje — reconciliar conteúdo, não só o visual, na Fase 4.

## Plano de execução (evita retrabalho)

Fundação antes de telas — se qualquer tela for feita antes da Fase 0, ela reimplementa nav/footer/
anúncio do próprio jeito e sobra retrabalho quando a fundação chegar depois.

- **Fase 0 (sequencial, obrigatória primeiro):** `SiteNav`, `SiteFooter`, `AdRail` (componente novo),
  `AdBannerWide` (substitui o `AdBanner.tsx` atual de slot único) — usando o mecanismo de catálogo/
  fallback já pronto dos PRs #1409/#1410.
- **Fase 1:** Home (`/`) — todas as variantes de `ScreenHome`.
- **Fase 2:** Histórico — incluindo o gráfico de evolução novo.
- **Fase 3:** Pro.
- **Fase 4:** Institucional (`ScreenDoc` + `Screen404`), incluindo `/como-medimos` nova e
  reconciliação de copy legal.

Fases 1-4 só começam depois da Fase 0 mergeada; entre si podem rodar em paralelo (arquivos de rota
diferentes) usando worktree para não conflitarem.

Dono: Lia (frente de design + frontend do Site).
