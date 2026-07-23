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
| `/quem-somos` | `QuemSomosPage` | Institucional |
| `/privacidade` | `PrivacidadePage` | Política de privacidade do site (distinta da do app) |
| `/termos` | `TermosPage` | Termos de uso do site (novo — não existia no protótipo) |
| `*` | `NotFoundPage` | 404 |

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
- Captura de e-mail da lista de espera do SignallQ PRO (`ProPage.tsx`) ainda não tem destino real
  (sem tabela D1/CRM decidido) — hoje só registra telemetria do clique.
- Deploy real em `signallq.pages.dev` ainda não foi executado nesta sessão (sem credenciais
  Cloudflare no ambiente de implementação) — workflow de CI/deploy está pronto, mas precisa dos
  secrets `CLOUDFLARE_ACCOUNT_ID`/`CLOUDFLARE_API_TOKEN` (já usados por outros workflows do repo)
  e da primeira execução real para validar contra produção.
