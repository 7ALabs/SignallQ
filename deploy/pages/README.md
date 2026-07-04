# Cloudflare Pages — projeto unificado `signallq`

Resolve GH#443 (duplicado: GH#440) e SIG-52.

## Decisão

O SignallQ WebApp/PWA (`pwa/`) e o SignallQ Console/Admin (`SignallQ Admin/`)
passam a ser publicados no **mesmo projeto Cloudflare Pages**, com rotas
explícitas:

```text
signallq.pages.dev/app       -> WebApp/PWA (pwa/)
signallq.pages.dev/console   -> Console/Admin (SignallQ Admin/)
signallq.pages.dev/          -> redirect 302 para /app
```

Antes desta mudança, os dois projetos eram publicados separadamente
(`signallq-admin-panel` já existia; `signallq-pwa` nunca chegou a existir —
ver `pwa/docs/deploy-status.md`). O projeto Cloudflare Pages único chamado
`signallq` é o que dá origem ao domínio `signallq.pages.dev` citado na issue.

## Como funciona

`build.mjs` builda os dois apps separadamente (cada um com seu próprio
`vite.config.ts`, cada um lendo `VITE_BASE_PATH` para gerar assets com o
prefixo certo) e depois monta um único diretório de saída:

```text
deploy/pages/dist/
  app/...       <- pwa/dist (build com VITE_BASE_PATH=/app/)
  console/...   <- SignallQ Admin/dist (build com VITE_BASE_PATH=/console/)
  _redirects    <- deploy/pages/_redirects
  _headers      <- deploy/pages/_headers
```

Esse diretório único é o `pages_build_output_dir` do projeto `signallq`
(ver `deploy/pages/wrangler.jsonc`).

## Build local

Pré-requisito: `npm ci` já rodado em `pwa/` e em `SignallQ Admin/`.

```bash
node deploy/pages/build.mjs
```

## Deploy manual

```bash
npx wrangler pages deploy deploy/pages/dist --project-name signallq
```

Requer `wrangler login` (ou `CLOUDFLARE_API_TOKEN` + `CLOUDFLARE_ACCOUNT_ID`
no ambiente) com permissão `pages:write` na conta correta.

## Roteamento SPA e redirect da raiz

`_redirects`:

- `/app` e `/console` (sem barra final) redirecionam 301 para a versão com
  barra — necessário porque o WebApp e o Console usam **hash routing**
  (`window.location.hash`), então o servidor só precisa saber servir o
  `index.html` de cada subpasta; não existem rotas de servidor por app.
- `/app/*` e `/console/*` caem em `index.html` (200) — cobre refresh direto
  e qualquer link profundo eventual.
- `/` redireciona (302) para `/app`.

## Headers de segurança

`_headers` aplica o mesmo conjunto de headers restritivos que já existia em
`pwa/public/_headers`, agora escopado para `/app/*` (e override de CSP para
`/app/docs/*`, usado pelo Swagger). **Não foi aplicado nenhum header novo a
`/console/*`** — isso é decisão do dono do Admin (Felipe), fora do escopo
desta issue. Hoje o Console é servido sem `_headers` próprio (padrão do
Cloudflare Pages), e esse comportamento foi preservado.

## Manifest, ícones e service worker

- `pwa/public/manifest.webmanifest` e `SignallQ Admin/public/manifest.json`
  usam caminhos relativos (`./icon-192.png`, `start_url: "./"`, etc.) — a
  spec de Web App Manifest resolve esses caminhos relativos à URL do
  manifest, então funcionam tanto em `/app/manifest.webmanifest` quanto em
  standalone (`/manifest.webmanifest`), sem precisar de outra config.
- `pwa/public/sw.js` e o registro do service worker em `pwa/src/main.tsx`
  passaram a usar caminhos relativos ao `scope` do worker
  (`self.registration.scope` / `import.meta.env.BASE_URL`), pelo mesmo
  motivo.

## O que NÃO está incluído nesta mudança

- **Criação do projeto Cloudflare Pages `signallq`** — isso é uma ação de
  conta/dashboard Cloudflare (ou `wrangler pages project create`), fora do
  alcance de um PR de código. Precisa ser feito por quem tem acesso à conta
  Cloudflare.
- **Configuração de build no dashboard** (se o projeto for conectado via
  Git em vez de `wrangler pages deploy` manual/CI): comando de build
  sugerido:
  ```bash
  cd pwa && npm ci && cd .. && cd "SignallQ Admin" && npm ci && cd .. && node deploy/pages/build.mjs
  ```
  Diretório de saída: `deploy/pages/dist`.
- **Variáveis de ambiente de build** — o build do Admin em modo produção
  falha propositalmente (GH#416) se `VITE_ADMIN_API_BASE_URL` estiver vazio
  ou `VITE_ENABLE_MOCKS=true`. Essas variáveis precisam existir no ambiente
  que roda `node deploy/pages/build.mjs` (dashboard do Cloudflare Pages ou
  secrets do GitHub Actions), com os mesmos valores já usados hoje para
  `signallq-admin-panel`.
- **Domínio custom + HTTPS/HSTS finais** (parte de SIG-52) — depende do
  projeto `signallq` existir primeiro; sem isso não há domínio para emitir
  certificado.

## CI

`.github/workflows/pages-deploy.yml` builda e tenta publicar o pacote
unificado em push para `main`, mas **só deploya se todos os secrets
necessários existirem** (`CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`,
`VITE_ADMIN_API_BASE_URL`). Se algum estiver ausente, o workflow builda
(valida que o pipeline não quebrou) e pula o deploy com log explícito — não
inventa um deploy que não aconteceu.

O job antigo `cloudflare-pages` em `pwa-ci.yml`, que tentava publicar o
`pwa/dist` isolado no projeto `signallq-pwa` (que nunca existiu), foi
removido para não competir com o deploy unificado.
