# SignallQ Site

Site institucional público do SignallQ — teste de velocidade real (sem simulação), histórico
local, páginas institucionais (Quem somos, SignallQ PRO, Privacidade, Termos). Superfície do
produto **SignallQ** (mesma linha do app Android e do Console), não um quarto produto — ver
`.claude/CLAUDE.md` (raiz do monorepo), tabela "Produtos Ativos".

## Stack

- **Next.js (App Router) + React 19 + TypeScript + Tailwind 4** — migrado de Vite/React Router em
  31/07-01/08/2026 (ver "Migração para Next.js" abaixo). Rotas em `src/app/<rota>/page.tsx`.
- PWA via `@serwist/next` (`next.config.ts` + `src/app/sw.ts` gera `public/sw.js`).
- Vitest + Testing Library para testes unitários.
- Deploy: Cloudflare Pages, projeto **`signallq`** (reaproveitado — estava desativado desde
  2026-07-16 quando o Console migrou para `signallq-admin-panel.pages.dev`, ver
  `docs_ai/operations/ADMIN_PANEL.md`). Domínio público alvo: `signallq.pages.dev` (ou domínio
  próprio, quando configurado em Cloudflare Pages → Custom domains).
- `AGENTS.md` (importado por este arquivo) avisa que a versão de Next.js em uso tem diferenças de
  API/convenção em relação ao conhecimento de treinamento do modelo — checar
  `node_modules/next/dist/docs/` antes de assumir comportamento padrão do framework.

## Estrutura

```
SignallQ Site/
├── src/
│   ├── app/            # rotas (App Router) — uma pasta por rota, page.tsx + layout.tsx/globals.css
│   ├── lib/             # config, motor de medição real, classificação, histórico (IndexedDB),
│   │                    # telemetria, SEO, matemática do velocímetro/gráfico — sem framework
│   ├── hooks/           # useSpeedTest (state machine do teste), useSystemTheme, useDocumentMeta
│   ├── components/      # SiteNav, SiteFooter, PageShell, AdRail/AdBannerWide, componentes de
│   │                    # speedtest/histórico
│   └── shared/          # tipos/contratos compartilhados com functions/ (chamado, genieacs, etc.)
├── functions/api/       # Cloudflare Pages Functions — backend server-side (telemetria, waitlist,
│                        # speedtest, admin, ERP, GenieACS; ver nota abaixo)
├── _archive_vite/        # snapshot do app Vite pré-migração (App.tsx, main.tsx, index.html,
│                          # src/pages/*, vite.config.ts) — mantido para rollback, não editar
└── public/               # ícones, manifest.json, robots.txt, sitemap.xml, _redirects, assets de marca
```

`functions/api/` cresceu além do escopo original de telemetria/waitlist descrito neste documento
(hoje também cobre auth do Admin, ERP, GenieACS, massiva) sem que a documentação tenha
acompanhado — pendência de doc conhecida, não introduzida por esta migração; atualizar quando
alguém tocar essa área.

## Origem

Implementado a partir de um protótipo Claude Design (Design Components — `.dc.html`) entregue
pela Lia — fonte viva: [SignallQ — Protótipos](https://claude.ai/design/p/e77ea465-291f-4bf5-930c-a267680da04e)
— seguindo o mesmo fluxo já usado para o Console (Lia desenha, Camilo implementa). O
protótipo assumia HTML estático puro; a decisão de arquitetura (registrada nas issues
#1147-#1155) trocou para stack React porque o próprio protótipo já importava
`@signallq/design-system` via React — HTML puro exigiria reimplementar à mão um design system que
já existe em React.

**Reconstrução v2 (2026-07-25):** decisão do Luiz de reconstruir o site 1:1 contra um protótipo
novo (`SignallQ Web - Prototipo`, `.claude/design-specs/2026-07-25-site-webapp-v2/`), em fases —
Fase 0 (`SiteNav`, `SiteFooter`, `AdRail`, `AdBannerWide`, fundação de anúncio local, PR #1412),
Fase 1 (Home, PR #1416), Fase 2 (Histórico), Fase 3 (PRO, PR #1413) e Fase 4 (institucional, PR
#1414) — todas mergeadas. As páginas institucionais (Como medimos, Quem somos, Privacidade,
Termos, Bufferbloat, CGNAT) e a 404 usam um template único, `DocPage.tsx`
(`sections`/`overline`/`title`/`intro`/`cta` configuráveis, igual ao componente
`ScreenDoc.dc.html` do protótipo, cuja prop `page` troca o conteúdo), composto dentro de
`PageLayout.tsx`/`PageShell.tsx` — que inclui a moldura de anúncio universal (`AdRail`
esquerda/direita só desktop + `AdBannerWide` embaixo do conteúdo, antes do `SiteFooter`).

**Migração para Next.js (31/07-01/08/2026):** reescrita de Vite+React Router para Next.js App
Router, feita numa sessão anterior sem issue/PR/ADR de registro — decisão confirmada com o Luiz em
01/08/2026 (ver `fix/site-nextjs-auditoria-mobile-shell`) como a direção correta a seguir,
substituindo o que ainda faltava do plano de reconstrução v2 em Vite. `src/pages/*` (React Router)
foi removido; equivalente vive em `src/app/*/page.tsx`. O app Vite anterior à migração foi
preservado em `_archive_vite/` para rollback. PWA passou a usar `@serwist/next` em vez do service
worker manual anterior. Auditoria 1:1 contra o protótipo mais recente (`SignallQ Web - Prototipo
(3)`, 01/08/2026) encontrou divergências reais pós-migração — ver PR de origem de
`fix/site-nextjs-auditoria-mobile-shell` para o detalhe (AdRail sem gate responsivo real,
hambúrguer do SiteNav morto, Histórico duplicando cabeçalho em mobile, entre outras).

## Decisões técnicas relevantes (não repetir sem reler o motivo)

- **Design system consumido via CSS puro, não via pacote React**: `packages/design-system/`
  nunca foi integrado a um app React de produção antes desta entrega — para não gastar o tempo
  do MVP1 depurando uma integração nunca testada, o site importa `tokens.css` direto
  (`src/app/globals.css`) e usa Tailwind para o resto. Se o pacote `@signallq/design-system` for
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
- **AdSense**: um único slot reservado, sem popup/banner extra, placeholder honesto quando não
  configurado.

## Rotas

| Rota | Página | Observação |
|---|---|---|
| `/` | `src/app/page.tsx` | Teste de velocidade real, auto-inicia ao carregar |
| `/pro` | `src/app/pro/page.tsx` | Vitrine do SignallQ PRO — "Em breve", sem promessa de trial |
| `/historico` | `src/app/historico/page.tsx` | Histórico local (IndexedDB) |
| `/como-medimos` | `src/app/como-medimos/page.tsx` | Metodologia |
| `/quem-somos` | `src/app/quem-somos/page.tsx` | Institucional |
| `/privacidade` | `src/app/privacidade/page.tsx` | Política de privacidade do site (distinta da do app) — 11 seções, inclui Cloudflare Web Analytics e lista de espera do PRO |
| `/termos` | `src/app/termos/page.tsx` | Termos de uso do site — 11 seções |
| `/internet-boa-mas-travando` | `src/app/internet-boa-mas-travando/page.tsx` | Conteúdo long-tail SEO (issue #1399) — H1 ancorado na frase sintomática, explica bufferbloat como causa |
| `/lag-em-jogos-online` | `src/app/lag-em-jogos-online/page.tsx` | Conteúdo long-tail SEO (issue #1399) — explica CGNAT/NAT Strict como causa de lag e falha ao hospedar partida |
| `*` | `src/app/not-found.tsx` | 404 — composição 1:1 com `Screen404.dc.html` |
| `/app`, `/brand`, `/comparativo`, `/internet-para-jogos`, `/teste` | `src/app/<rota>/page.tsx` | Rotas adicionadas depois da última revisão deste doc — pendente de descrição própria |

Páginas de conteúdo long-tail seguem "resposta primeiro" (cada seção responde a pergunta do título
já nas 1-2 primeiras frases — única recomendação de formato validada pela consultoria de SEO em
#1374), linkam pro teste de velocidade (`/`) e uma pra outra quando faz sentido, e usam
`src/lib/pageMetaCatalog.ts` para SEO técnico, com builder de Article JSON-LD
(`buildArticleJsonLd` em `src/lib/structuredData.ts`) específico pra conteúdo editorial — decisão
registrada em #1399.

## Comandos

```bash
npm install
npm run dev       # next dev --webpack
npm run test      # vitest run
npm run lint      # eslint
npm run build     # next build
```

## Pendências conhecidas (ver PR de origem para detalhe completo)

- `SITE_INGEST_KEY` (ou reaproveitar `INGEST_KEY` do app) precisa ser configurada como secret do
  projeto Cloudflare Pages `signallq` — decisão/execução do Luiz, não é código.
- O SignallQ gratuito encaminha para o grupo de testadores fechados, sem capturar e-mail. A lista
  de espera do SignallQ PRO (`src/app/pro/page.tsx`) persiste em D1 via
  `functions/api/waitlist.ts`, mas requer `SITE_INGEST_KEY` e migration remota configuradas para
  funcionar em produção.
- Divergências da auditoria 1:1 pós-migração Next.js (AdRail sem gate responsivo real, hambúrguer
  do SiteNav morto, Histórico duplicando cabeçalho em mobile, anúncios ausentes no estado de
  resultado, texto de "Interpretação SignallQ" não variando por classificação, métricas de
  detalhes técnicos hardcoded em "—", `PageShell`/`PageLayout` duplicados) — ver PR de
  `fix/site-nextjs-auditoria-mobile-shell`.
- `functions/api/` cresceu sem acompanhamento de documentação (ver nota em "Estrutura") — precisa
  de auditoria própria antes de virar dívida maior.
