# Cloudflare Pages — projeto `signallq`

Resolve GH#443 (duplicado: GH#440) e SIG-52.

## Decisão

O SignallQ Console/Admin (`SignallQ Admin/`) é publicado no projeto
Cloudflare Pages `signallq`, sob a rota:

```text
signallq.pages.dev/console   -> Console/Admin (SignallQ Admin/)
```

O PWA/WebApp (`pwa/`) que originalmente era publicado sob `/app` neste mesmo
projeto foi descontinuado e removido do monorepo. As entradas de `/app` em
`_redirects` e `_headers` ficaram órfãs e precisam de revisão separada antes
do próximo deploy (ver seção "O que NÃO está incluído nesta mudança").

## Como funciona

`build.mjs` builda o Console Admin (lendo `VITE_BASE_PATH=/console/` para
gerar assets com o prefixo certo) e monta o diretório de saída:

```text
deploy/pages/dist/
  console/...   <- SignallQ Admin/dist (build com VITE_BASE_PATH=/console/)
  _redirects    <- deploy/pages/_redirects
  _headers      <- deploy/pages/_headers
```

Esse diretório único é o `pages_build_output_dir` do projeto `signallq`
(ver `deploy/pages/wrangler.jsonc`).

## Build local

Pré-requisito: `npm ci` já rodado em `SignallQ Admin/`.

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

`_redirects` e `_headers` ainda contêm entradas de `/app` herdadas da época
do PWA. Elas não quebram o build (o script não lê esses arquivos), mas
precisam ser revisadas — hoje `/` ainda redireciona para `/app`, que não
existe mais. Ajuste fora do escopo desta limpeza de CI.

## Headers de segurança

`_headers` hoje só tem regras para `/app/*`, herdadas do PWA descontinuado.
Não há headers próprios configurados para `/console/*` — isso é decisão do
dono do Admin (Felipe), fora do escopo desta issue. Hoje o Console é servido
sem `_headers` próprio (padrão do Cloudflare Pages).

## O que NÃO está incluído nesta mudança

- **Ajuste de `_redirects`/`_headers`** para remover as entradas órfãs de
  `/app` e decidir o novo destino do redirect da raiz (`/console`?) — precisa
  de decisão de produto, não é puramente mecânico.
- **Criação do projeto Cloudflare Pages `signallq`** — isso é uma ação de
  conta/dashboard Cloudflare (ou `wrangler pages project create`), fora do
  alcance de um PR de código. Precisa ser feito por quem tem acesso à conta
  Cloudflare.
- **Configuração de build no dashboard** (se o projeto for conectado via
  Git em vez de `wrangler pages deploy` manual/CI): comando de build
  sugerido:
  ```bash
  cd "SignallQ Admin" && npm ci && cd .. && node deploy/pages/build.mjs
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

`.github/workflows/pages-deploy.yml` builda e tenta publicar o Console Admin
em push para `main`, mas **só deploya se todos os secrets necessários
existirem** (`CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`,
`VITE_ADMIN_API_BASE_URL`). Se algum estiver ausente, o workflow builda
(valida que o pipeline não quebrou) e pula o deploy com log explícito — não
inventa um deploy que não aconteceu.
