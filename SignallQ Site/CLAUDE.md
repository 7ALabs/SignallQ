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
│   ├── components/      # SiteNav, SiteFooter, PageShell, CookieConsentBanner/AdSenseScript,
│   │                    # componentes de speedtest/histórico
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

**Reconstrução v4 (01/08/2026, `refactor/site-reconstrucao-prototipo-v4`, issues #1543/#1544/#1545):**
reconstrução tela a tela 1:1 contra um protótipo novo (`SignallQ Web - Prototipo (4)`, guia de
implementação próprio, arquivo local do Luiz não versionado no repo), em workflow sequencial
(fundação → Home → Histórico → Doc → App → Brand → 404), cada fase com implementação + revisão
independente. Decisões de escopo confirmadas com o Luiz nesta rodada:
- `AdRail`/`AdBannerWide`/`AdSlotsProvider` (Feat #1402) removidos do layout visual — o protótipo
  v4 não reserva mais posição de anúncio. Substituídos por infraestrutura técnica de AdSense sem
  posição decidida: `CookieConsentBanner` (consentimento LGPD, aceitar/recusar persistido) +
  `AdSenseScript` (`next/script`, só carrega com `NEXT_PUBLIC_ADSENSE_PUBLISHER_ID` configurada E
  consentimento aceito — sem a env var, comportamento visual não muda). `public/ads.txt` já tinha o
  publisher ID real do Luiz (`pub-5542349230926522`, commitado em jul/2026) — mantido como estava,
  não foi sobrescrito.
- `/pro` (vitrine SignallQ PRO) e `/teste` (convite pro teste fechado) removidos — não estavam no
  mapa de rotas do protótipo v4. Redirect 301 em `next.config.ts`: `/pro` → `/`, `/teste` → `/app`.
  `EmailCaptureDialog.tsx` removido por ter ficado órfão.
- `PageLayout.tsx`/`PageShell.tsx`, que estavam duplicados (pendência documentada abaixo antes desta
  entrada), foram consolidados numa fundação única `PageShell`.
- Componentes de produto renomeados para PT-BR: `SpeedDial` → `Velocimetro`, `EmptyState` →
  `EstadoVazio`, `KeyValueList` → `ListaChaveValor`; novos `FaixaMetricas`/`LinhaChips` extraídos
  (reaproveitados por Home/Histórico/404).
- `src/app/page.tsx` (Home) religou `jitter`/`bufferbloat`/`estabilidade`/`DNS` já calculados em
  `speedEngine.ts` (achado da auditoria de 01/08/2026 abaixo, agora resolvido) — a implementação
  antiga em `components/speedtest/*` que consumia esses campos estava órfã e foi removida.

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
- **AdSense**: desde a reconstrução v4 (01/08/2026), sem posição de anúncio no layout — só
  infraestrutura técnica pronta (`ads.txt`, `AdSenseScript` gated por env var + consentimento,
  `CookieConsentBanner`). Ativar exige decidir a posição do slot (fora de escopo desta rodada) e
  confirmar com o Luiz que `NEXT_PUBLIC_ADSENSE_PUBLISHER_ID` deve ir para produção.

## Rotas

| Rota | Página | Observação |
|---|---|---|
| `/` | `src/app/page.tsx` | Teste de velocidade real, auto-inicia ao carregar |
| `/historico` | `src/app/historico/page.tsx` | Histórico local (IndexedDB) — `noindex,follow`, fora do sitemap (dado local, sem conteúdo pro Google) |
| `/como-medimos` | `src/app/como-medimos/page.tsx` | Metodologia |
| `/quem-somos` | `src/app/quem-somos/page.tsx` | Institucional |
| `/privacidade` | `src/app/privacidade/page.tsx` | Política de privacidade do site (distinta da do app) — 10 seções (a seção "Lista de espera do PRO" saiu junto com `/pro` na reconstrução v4) |
| `/termos` | `src/app/termos/page.tsx` | Termos de uso do site — 11 seções |
| `/internet-boa-mas-travando` | `src/app/internet-boa-mas-travando/page.tsx` | Conteúdo long-tail SEO (issue #1399) — H1 ancorado na frase sintomática, explica bufferbloat como causa |
| `/lag-em-jogos-online` | `src/app/lag-em-jogos-online/page.tsx` | Conteúdo long-tail SEO (issue #1399) — explica CGNAT/NAT Strict como causa de lag e falha ao hospedar partida |
| `/internet-para-jogos` | `src/app/internet-para-jogos/page.tsx` | Guia de conteúdo por caso de uso (gaming) |
| `/comparativo` | `src/app/comparativo/page.tsx` | SignallQ x testes de velocidade tradicionais |
| `/app` | `src/app/app/page.tsx` | Landing do app Android em teste fechado — também recebe o redirect de `/teste` |
| `/brand` | `src/app/brand/page.tsx` | Página institucional de marca (logo, paleta, favicons) |
| `*` | `src/app/not-found.tsx` | 404 — composição 1:1 com `Screen404.dc.html` |

`/pro` (vitrine SignallQ PRO) e `/teste` (convite pro teste fechado) foram **removidas** na
reconstrução v4 (01/08/2026) — não estavam no mapa de rotas do protótipo novo. Redirecionam (301,
`next.config.ts`) para `/` e `/app` respectivamente, pra não perder link externo/indexação já
publicada.

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
  de espera do SignallQ PRO persistia em D1 via `src/app/api/waitlist/route.ts` — desde a remoção
  de `/pro` na reconstrução v4 (01/08/2026), essa Route Handler ficou **órfã** (nenhuma UI chama
  `submitWaitlistSignup`). Não removida (é `src/app/api/*/route.ts`, backend, fora do escopo da PR
  de frontend que fez a remoção) — decidir em PR própria se a lista de espera do PRO volta em outro
  lugar ou se a rota é descartada. Mesma situação em `functions/api/waitlist.ts` (Pages Function
  equivalente, rede de segurança pré-cutover Vercel).
- `functions/_middleware.ts` (Cloudflare Pages Function) ainda trata `/pro` como rota válida
  (`buildProSoftwareApplicationJsonLd`, achado 01/08/2026 na reconstrução v4) — hoje é só peso morto
  (o path responde via redirect 301 do Next.js antes de qualquer render, o JSON-LD nunca é servido
  de verdade), não um bug funcional. Não corrigido por ser `functions/` (backend) — limpar junto da
  waitlist órfã acima, numa PR de backend.
- **RESOLVIDO na reconstrução v4 (01/08/2026):** anúncios ausentes / `PageShell`/`PageLayout`
  duplicados / métricas reais órfãs (`ResultPanel.tsx`/`EmbeddedSpeedTest.tsx`) — o protótipo v4
  removeu a posição de anúncio do design (não é mais divergência), `PageShell`/`PageLayout` foram
  consolidados num único componente, e a Home foi religada direto aos campos reais já calculados em
  `speedEngine.ts` (jitter/bufferbloat/estabilidade/DNS) — os componentes órfãos antigos foram
  removidos, não mantidos em paralelo. Os 3 achados estruturais de mobile (AdRail sem gate
  responsivo real, hambúrguer do SiteNav morto, Histórico duplicando cabeçalho em mobile) foram
  corrigidos em `fix/site-nextjs-auditoria-mobile-shell` (01/08/2026): `PageShell`/`SiteNav`/`AdRail`
  deixaram de depender de uma prop `mobile` que nenhuma página ligava a um viewport real e passaram
  a gate CSS puro (`hidden lg:flex` no `AdRail`, `md:hidden`/`hidden md:flex` no `SiteNav`,
  consistente com o padrão já usado em `historico/page.tsx` e `SiteFooter.tsx`); o hambúrguer ganhou
  um menu mobile funcional (drawer com os mesmos itens da nav desktop, fecha ao navegar ou Esc) em
  vez de ser removido. (`AdRail` citado aqui como registro histórico — removido na reconstrução v4.)
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
- **`npm run lint` reporta 103 erros / 1013 warnings pré-existentes** (baseline original 106/1022 em
  01/08/2026 pré-v4; reconfirmado após a reconstrução v4 — nenhuma regressão líquida introduzida,
  a diferença vem de arquivos removidos na v4, não de correções de lint deliberadas). Maioria vem
  de regras novas e rígidas do React Compiler ESLint
  plugin (`eslint-config-next` 16), ex. `react-hooks/set-state-in-effect` e `react-hooks/purity`
  em `src/hooks/useEstadoRede.ts`, `src/hooks/useSpeedTest.ts`, `src/lib/speedEngine.ts` — padrões
  de código legados que passavam nas regras antigas do Vite/eslint anterior. Precisa de auditoria
  dedicada (avaliar caso a caso se é falso positivo do React Compiler ou refactor real necessário)
  antes de tratar `npm run lint` como gate obrigatório de CI.
- `functions/api/` cresceu sem acompanhamento de documentação (ver nota em "Estrutura") — precisa
  de auditoria própria antes de virar dívida maior.
