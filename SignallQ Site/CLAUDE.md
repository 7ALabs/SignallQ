# SignallQ Site

Site institucional público do SignallQ — teste de velocidade real (sem simulação), histórico
local, páginas institucionais (Quem somos, SignallQ PRO, Privacidade, Termos). Superfície do
produto **SignallQ** (mesma linha do app Android e do Console), não um quarto produto — ver
`.claude/CLAUDE.md` (raiz do monorepo), tabela "Produtos Ativos".

## Stack

- **Next.js (App Router) + React 19 + TypeScript + Tailwind 4** — migrado de Vite/React Router em
  31/07-01/08/2026 (ver "Migração para Next.js" abaixo). Rotas em `src/app/<rota>/page.tsx`.
- PWA via `@serwist/next` (`next.config.ts` + `src/app/sw.ts` gera `public/sw.js`).
- Vitest + Testing Library para testes unitários — **infra descontinuada na migração Next.js, ver
  pendência "Infra de teste ausente" abaixo; `npm run test` não existe hoje.**
- Deploy: **migrando de Cloudflare Pages para Vercel** (decisão do Luiz, 01/08/2026 — ver seção
  "Hospedagem: migração Cloudflare Pages → Vercel" abaixo). Até o cutover, o deploy ativo continua
  no Cloudflare Pages, projeto **`signallq`** (reaproveitado — estava desativado desde 2026-07-16
  quando o Console migrou para `signallq-admin-panel.pages.dev`, ver
  `docs_ai/operations/ADMIN_PANEL.md`), domínio `signallq.pages.dev`.
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

## Hospedagem: migração Cloudflare Pages → Vercel (decisão do Luiz, 01/08/2026)

Além da migração de framework (Vite→Next.js, ver acima), o site está migrando de hospedagem
**Cloudflare Pages → Vercel**. Vercel não executa `functions/api/*` (mecanismo específico do
Cloudflare Pages) — por isso:

- `functions/api/track.ts` e `functions/api/waitlist.ts` (chamados de verdade pelo app real, ver
  `TELEMETRY_ENDPOINT`/`WAITLIST_ENDPOINT` em `src/lib/config.ts`) foram portados para Next.js
  Route Handlers em `src/app/api/track/route.ts` e `src/app/api/waitlist/route.ts`
  (`fix/site-nextjs-producao-motores`, 01/08/2026) — mesmo comportamento (proxy server-side pro
  `signallq-admin-worker`, `SITE_INGEST_KEY` nunca client-side), só trocando `context.env.X`
  (Cloudflare Pages Functions) por `process.env.X` (Next.js/Vercel). Validado localmente: com
  `SITE_INGEST_KEY` configurada, `/api/track` e `/api/waitlist` alcançam de verdade o
  `signallq-admin-worker` de produção (confirmado via `401 Unauthorized` com chave de teste —
  prova que o proxy funciona ponta a ponta, não é mock). As Pages Functions originais foram
  mantidas (não removidas) como rede de segurança enquanto o Cloudflare Pages ainda estiver no ar
  — remover só depois do cutover confirmado para Vercel.
- `functions/api/speedtest/*` (`download.ts`, `upload.ts`, `latency.ts`, `dns.ts`) são dead code —
  nada em `src/` os chama; o motor real fala direto com `speed.cloudflare.com` e o worker de
  latência (ver `src/lib/speedEngine.ts`/`src/lib/config.ts`). Não foram portados de propósito —
  serão descartados junto com o Cloudflare Pages.
- `functions/api/admin/*`, `functions/api/erp/*`, `functions/api/genieacs/*`,
  `functions/api/massiva/*`, `functions/api/assinante/cpf.ts`, `functions/api/diagnostico/3a.ts`
  **não são código do Site** — são o deploy manual do tenant Leste Telecom do Agente Virtual
  (produto irmão, hoje em backlog), publicado em `signallq.pages.dev/leste`, compartilhando o
  projeto Cloudflare Pages por conveniência (PR #1281). Fora de escopo de qualquer migração do
  Site — é decisão de infra separada do Luiz.

## Pendências conhecidas (ver PR de origem para detalhe completo)

- `SITE_INGEST_KEY` (ou reaproveitar `INGEST_KEY` do app) precisa ser configurada como env var/secret
  do projeto de hospedagem (Vercel — ou, enquanto ainda no ar, Cloudflare Pages `signallq`) —
  decisão/execução do Luiz, não é código.
- O SignallQ gratuito encaminha para o grupo de testadores fechados, sem capturar e-mail. A lista
  de espera do SignallQ PRO (`src/app/pro/page.tsx`) persiste em D1 via
  `src/app/api/waitlist/route.ts`, mas requer `SITE_INGEST_KEY` e migration remota configuradas
  para funcionar em produção.
- Divergências restantes da auditoria 1:1 pós-migração Next.js (anúncios ausentes no estado de
  resultado, texto de "Interpretação SignallQ" não variando por classificação, métricas de
  detalhes técnicos hardcoded em "—" na home — **`src/components/speedtest/ResultPanel.tsx` +
  `EmbeddedSpeedTest.tsx` já implementam essas métricas com dado real (jitter/bufferbloat/
  estabilidade/DNS), mas estão órfãos, sem nenhum consumidor em `src/app/` — achado 01/08/2026,
  candidato natural pra resolver essa pendência sem reescrever o cálculo, só rewiring da home**,
  `PageShell`/`PageLayout` duplicados) — os 3 achados estruturais de mobile (AdRail sem gate
  responsivo real, hambúrguer do SiteNav morto, Histórico duplicando cabeçalho em mobile) foram
  corrigidos em `fix/site-nextjs-auditoria-mobile-shell` (01/08/2026): `PageShell`/`SiteNav`/`AdRail`
  deixaram de depender de uma prop `mobile` que nenhuma página ligava a um viewport real e passaram
  a gate CSS puro (`hidden lg:flex` no `AdRail`, `md:hidden`/`hidden md:flex` no `SiteNav`,
  consistente com o padrão já usado em `historico/page.tsx` e `SiteFooter.tsx`); o hambúrguer ganhou
  um menu mobile funcional (drawer com os mesmos itens da nav desktop, fecha ao navegar ou Esc) em
  vez de ser removido.
- **Infra de teste ausente (achado 01/08/2026, fora do escopo da correção de mobile shell acima):**
  `package.json` não tem script `test`, e `vitest`/`@testing-library/react`/`react-router-dom`
  não estão em `package.json`/`package-lock.json`/`node_modules` — a migração Vite→Next.js
  (31/07-01/08/2026) descontinuou a infra de teste sem que ninguém percebesse. Os 16 arquivos
  `*.test.tsx`/`*.test.ts` em `src/` (ex.: `SiteNav.test.tsx`, `SiteFooter.test.tsx`,
  `DocPage.test.tsx`) ainda importam `MemoryRouter` de `react-router-dom` (rota que não existe
  mais no app real, que usa `next/navigation`) e não rodam — este documento ainda lista
  `npm run test` como comando válido, o que hoje é falso. Precisa de decisão dedicada (reinstalar
  Vitest + Testing Library, reescrever os 16 testes para Next.js) antes de reativar cobertura —
  não foi resolvido aqui por ser escopo maior que a correção pontual de mobile shell.
- **`npm run lint` reporta 106 erros / 1022 warnings pré-existentes** (achado 01/08/2026, não
  introduzido por `fix/site-nextjs-producao-motores` — confirmado que nenhum erro/warning cai em
  arquivo tocado nesta branch). Maioria vem de regras novas e rígidas do React Compiler ESLint
  plugin (`eslint-config-next` 16), ex. `react-hooks/set-state-in-effect` e `react-hooks/purity`
  em `src/hooks/useEstadoRede.ts`, `src/hooks/useSpeedTest.ts`, `src/lib/speedEngine.ts` — padrões
  de código legados que passavam nas regras antigas do Vite/eslint anterior. Precisa de auditoria
  dedicada (avaliar caso a caso se é falso positivo do React Compiler ou refactor real necessário)
  antes de tratar `npm run lint` como gate obrigatório de CI.
- `functions/api/` cresceu sem acompanhamento de documentação (ver nota em "Estrutura") — precisa
  de auditoria própria antes de virar dívida maior.
