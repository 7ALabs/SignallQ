# SignallQ Site

Site institucional público do SignallQ — teste de velocidade real (sem simulação), histórico
local, páginas institucionais (Quem somos, SignallQ PRO, Privacidade, Termos). Superfície do
produto **SignallQ** (mesma linha do app Android e do Console), não um quarto produto — ver
`.claude/CLAUDE.md` (raiz do monorepo), tabela "Produtos Ativos".

## Stack

- Vite + React 19 + TypeScript + Tailwind 4 (`@tailwindcss/vite`) — mesmo padrão do
  `SignallQ Admin/` e do Agente Virtual.
- React Router (`react-router-dom`) para as rotas client-side.
- Vitest + Testing Library para testes unitários.
- Deploy: Cloudflare Pages, projeto **`signallq`** (reaproveitado — estava desativado desde
  2026-07-16 quando o Console migrou para `signallq-admin-panel.pages.dev`, ver
  `docs_ai/operations/ADMIN_PANEL.md`). Domínio público alvo: `signallq.pages.dev` (ou domínio
  próprio, quando configurado em Cloudflare Pages → Custom domains).

## Estrutura

```
SignallQ Site/
├── src/
│   ├── lib/           # config, motor de medição real, classificação, histórico (IndexedDB),
│   │                  # telemetria, SEO, matemática do velocímetro/gráfico — sem framework
│   ├── hooks/         # useSpeedTest (state machine do teste), useSystemTheme, useDocumentMeta
│   ├── components/    # SiteNav, SiteFooter, AdSlot, Logo, componentes de speedtest/histórico
│   └── pages/         # uma página por rota
├── functions/api/     # Cloudflare Pages Functions (proxy server-side de telemetria)
└── public/            # ícones, manifest.json, robots.txt, sitemap.xml, _redirects, assets de marca
```

## Origem

Implementado a partir de um protótipo Claude Design (Design Components — `.dc.html`) entregue
pela Lia — fonte viva: [SignallQ — Protótipos](https://claude.ai/design/p/e77ea465-291f-4bf5-930c-a267680da04e)
— seguindo o mesmo fluxo já usado para o Console (Lia desenha, Camilo implementa). O
protótipo assumia HTML estático puro; a decisão de arquitetura (registrada nas issues
#1147-#1155) trocou para este stack porque o próprio protótipo já importava
`@signallq/design-system` via React — HTML puro exigiria reimplementar à mão um design system que
já existe em React.

**Reconstrução v2 (2026-07-25):** decisão do Luiz de reconstruir o site 1:1 contra um protótipo
novo (`SignallQ Web - Prototipo`, `.claude/design-specs/2026-07-25-site-webapp-v2/`), em fases —
Fase 0 (`SiteNav`, `SiteFooter`, `AdRail`, `AdBannerWide`, fundação de anúncio local, PR #1412),
Fase 1 (Home), Fase 2 (Histórico), Fase 3 (PRO) e Fase 4 (institucional — este documento). As
páginas institucionais (Como medimos, Quem somos, Privacidade, Termos, Bufferbloat, CGNAT) e a 404
passaram a usar um template único, `DocPage.tsx` (`sections`/`overline`/`title`/`intro`/`cta`
configuráveis, igual ao componente `ScreenDoc.dc.html` do protótipo, cuja prop `page` troca o
conteúdo), composto dentro de `PageLayout.tsx` — que agora inclui a moldura de anúncio universal
(`AdRail` esquerda/direita só desktop + `AdBannerWide` embaixo do conteúdo, antes do `SiteFooter`).

## Decisões técnicas relevantes (não repetir sem reler o motivo)

- **Design system consumido via CSS puro, não via pacote React**: `packages/design-system/`
  nunca foi integrado a um app React de produção antes desta entrega — para não gastar o tempo
  do MVP1 depurando uma integração nunca testada, o site importa `tokens.css` direto
  (`src/index.css`) e usa Tailwind para o resto. Se o pacote `@signallq/design-system` for
  validado em produção depois, reavaliar a troca.
- **Classificação (Boa/Aceitável/Ruim)**: portada 1:1 dos cortes reais em produção no app Android
  (`SpeedtestQualityClassifier.kt`, `ResultadoVelocidadeScreen.kt`), não da tabela provisória de 4
  níveis que o protótipo tinha inventado sem fonte oficial. Ver comentários em
  `src/lib/classification.ts`.
- **Telemetria server-side**: eventos de produto (`screen_view`, `feature_used`) vão para
  `functions/api/track.ts` (Pages Function), que repassa para
  `signallq-admin-worker`'s `POST /ingest/analytics` com `platform: 'web'`. A `INGEST_KEY` nunca
  aparece em código client-side — é secret do projeto Cloudflare Pages
  (`SITE_INGEST_KEY`, pendente de configuração real pelo Luiz). Nenhum vocabulário GA4 novo — os
  `feature_id` reaproveitam o funil de speedtest já existente no Console (GH#784).
- **Cloudflare Web Analytics** (não GA4) cobre tráfego/pageview agregado — habilitar direto no
  dashboard do projeto Cloudflare Pages, sem código.
- **Histórico**: IndexedDB (`src/lib/historyStore.ts`), só no navegador, sem sincronização.
- **AdSense**: um único slot reservado (`AdSlot.tsx`), sem popup/banner extra, placeholder
  honesto quando não configurado.

## Rotas

| Rota | Página | Observação |
|---|---|---|
| `/` | `HomePage` | Teste de velocidade real, auto-inicia ao carregar |
| `/pro` | `ProPage` | Vitrine do SignallQ PRO — "Em breve", sem promessa de trial |
| `/historico` | `HistoricoPage` | Histórico local (IndexedDB) |
| `/como-medimos` | `ComoMedimosPage` | Metodologia — rota nova da reconstrução v2 (não existia antes) |
| `/quem-somos` | `QuemSomosPage` | Institucional |
| `/privacidade` | `PrivacidadePage` | Política de privacidade do site (distinta da do app) — copy reconciliado 1:1 com o protótipo v2 na Fase 4 (11 seções, inclui Cloudflare Web Analytics e lista de espera do PRO) |
| `/termos` | `TermosPage` | Termos de uso do site — copy reconciliado 1:1 com o protótipo v2 na Fase 4 (11 seções); antes da Fase 4 era uma adaptação de `docs_ai/legal/TERMS_OF_USE.md` por falta de spec própria, hoje o protótipo já traz `Termos` |
| `/internet-boa-mas-travando` | `BufferbloatPage` | Conteúdo long-tail SEO (issue #1399) — H1 ancorado na frase sintomática, explica bufferbloat como causa. Copy preservado 1:1 na Fase 4 (só a moldura visual mudou) |
| `/lag-em-jogos-online` | `CgnatPage` | Conteúdo long-tail SEO (issue #1399) — explica CGNAT/NAT Strict como causa de lag e falha ao hospedar partida. Copy preservado 1:1 na Fase 4 (só a moldura visual mudou) |
| `*` | `NotFoundPage` | 404 — composição 1:1 com `Screen404.dc.html` na Fase 4 |

Ambas as páginas de conteúdo long-tail seguem "resposta primeiro" (cada seção responde a
pergunta do título já nas 1-2 primeiras frases — única recomendação de formato validada pela
consultoria de SEO em #1374), linkam pro teste de velocidade (`/`) e uma pra outra quando faz
sentido, e usam o mesmo mecanismo de SEO técnico da Fase 2 (`functions/_middleware.ts` +
`src/lib/pageMetaCatalog.ts`) das demais rotas — decisão registrada em #1399: reaproveitar a
injeção via HTMLRewriter em vez de pre-render dedicado, com o acréscimo de um builder de Article
JSON-LD (`buildArticleJsonLd` em `src/lib/structuredData.ts`) específico pra conteúdo editorial.

## Comandos

```bash
npm install
npm run dev       # http://localhost:3100
npm run test      # vitest run
npm run lint      # tsc --noEmit
npm run build     # tsc --noEmit && vite build
```

## Pendências conhecidas (ver PR de origem para detalhe completo)

- `SITE_INGEST_KEY` (ou reaproveitar `INGEST_KEY` do app) precisa ser configurada como secret do
  projeto Cloudflare Pages `signallq` — decisão/execução do Luiz, não é código.
- O SignallQ gratuito encaminha para o grupo de testadores fechados, sem capturar e-mail. A lista
  de espera do SignallQ PRO (`ProPage.tsx`) persiste em D1 via `functions/api/waitlist.ts`, mas
  requer `SITE_INGEST_KEY` e migration remota configuradas para funcionar em produção.
- ~~O deploy em `signallq.pages.dev` depende dos secrets `CLOUDFLARE_ACCOUNT_ID` e
  `CLOUDFLARE_API_TOKEN`~~ — resolvido em 2026-07-25: os dois secrets estão configurados no
  GitHub Actions. `site-deploy.yml` dispara automaticamente a cada push em `main` que toca
  `SignallQ Site/**`, sem passo manual.
