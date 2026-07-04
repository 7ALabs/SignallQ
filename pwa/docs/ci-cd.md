# CI/CD Cloudflare Pages

## Objetivo

Definir o contrato real de validacao, preview e deploy do SignallQ PWA sem misturar pipeline Android e sem prometer Cloudflare pronto quando a conta/projeto ainda nao batem.

## Atualizacao 2026-07-04 (GH#443 / SIG-52)

O deploy de producao deste app **deixou de ser isolado**. O PWA e o Console
Admin agora sao publicados juntos no mesmo projeto Cloudflare Pages, chamado
`signallq`, sob rotas explicitas:

```text
signallq.pages.dev/app       -> este app (pwa/)
signallq.pages.dev/console   -> SignallQ Admin/
signallq.pages.dev/          -> redirect 302 para /app
```

Fonte de verdade dessa configuracao: `deploy/pages/` (build script, wrangler
config, `_headers`, `_redirects`) e `deploy/pages/README.md`. O restante
deste documento descreve o pipeline de validacao (`pwa-ci.yml`), que
continua existindo e restrito a `pwa/**`, mas o job antigo de deploy
standalone (`cloudflare-pages` publicando em `signallq-pwa`) foi removido —
ver `.github/workflows/pages-deploy.yml`.

## Estado atual em 2026-06-30

Ja existe workflow em `.github/workflows/pwa-ci.yml` com gatilho restrito a `pwa/**` e ao proprio workflow.

Esse workflow agora:

- usa `working-directory: pwa`;
- executa `npm ci`;
- executa `npm run lint` apenas se o script existir;
- executa `npm run verify`;
- publica `pwa/dist/` como artifact;
- tenta preview/deploy no Cloudflare Pages apenas quando `CLOUDFLARE_API_TOKEN` e `CLOUDFLARE_ACCOUNT_ID` estiverem configurados.

O workflow nao tenta rodar pipeline Android. A excecao fora de `pwa/` continua restrita a `.github/workflows/pwa-ci.yml`, autorizada para a entrega do PWA.

## Projeto Pages

Configuracao versionada:

- arquivo: `pwa/wrangler.jsonc`;
- projeto esperado: `signallq-pwa`;
- output: `dist`;
- build command: `npm run build`;
- compatibility date: `2026-06-25`.

Status real verificado com Wrangler em 2026-06-30:

- autenticacao Cloudflare: OK;
- conta autenticada: `Giammattey, Luiz F.` (`2f38f7354f204d7b3f7d6c750b3e43ff`);
- permissao `pages:write`: OK;
- projeto `signallq-pwa`: NAO encontrado nessa conta;
- efeito pratico: `npm run pages:deploy` falha com `Project not found` e preview/producao nao podem ser marcados como validados.

## Scripts operacionais

```bash
npm run typecheck
npm test
npm run build
npm run verify
npm run pages:dev
npm run pages:deploy
```

Detalhes:

- `npm run verify` executa `npm run typecheck`, `npm test` e `npm run build`;
- `npm run pages:deploy` publica `dist` para o projeto `signallq-pwa`;
- `npm run lint` continua ausente e deve ser tratado como pendencia de infraestrutura, nao como validacao concluida.

## Workflow GitHub Actions

Arquivo:

```text
.github/workflows/pwa-ci.yml
```

Gatilhos:

- `pull_request` para `main` quando houver mudanca em `pwa/**` ou no proprio workflow;
- `push` em `main` quando houver mudanca em `pwa/**` ou no proprio workflow.

Job `Build & Test`:

- `npm ci`;
- `npm run lint`, somente se o script existir;
- `npm run verify`;
- upload de `pwa/dist` como artefato.

Job `Cloudflare Pages Preview/Deploy`:

- roda somente depois da validacao;
- em PR, publica preview usando a branch do PR quando os secrets existem;
- em push para `main`, publica o build da branch `main` quando os secrets existem;
- se os secrets nao existirem, registra skip e nao quebra a validacao basica do PR.

O workflow usa `working-directory: pwa`, nao aciona build Android e chama o script existente `npm run pages:deploy` para manter um unico contrato de deploy.

## Variaveis e secrets

Variaveis server-side esperadas no Cloudflare Pages:

- `AI_WORKER_URL`
- `ADMIN_INGEST_URL` ou `ADMIN_WORKER_URL`
- `ADMIN_INGEST_KEY`

Secrets esperados no GitHub Actions para deploy:

- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN`

Para desenvolvimento local com `wrangler pages dev`, use `.dev.vars`. Esse arquivo nao deve ser versionado.

Variaveis com prefixo `VITE_` sao publicas por definicao e nao devem receber segredo, token, ingest key ou URL privada que dependa de autenticacao.

## Headers e seguranca

O deploy statico passa a carregar `pwa/public/_headers` com:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy` restritiva
- `Content-Security-Policy` para a app e override mais permissivo em `/docs/*` por causa do Swagger CDN

HSTS continua pendente de validacao em dominio HTTPS final, portanto nao foi forcado neste momento.

## Fluxo recomendado

1. `npm ci`
2. `npm run verify`
3. `npm run pages:dev`
4. abrir PR e deixar o workflow publicar preview se os secrets existirem
5. publicar producao com `npm run pages:deploy` somente depois de existir o projeto Pages correto

## Criterios de aceite desta rodada

- `npm run typecheck`: passou
- `npm test`: passou
- `npm run build`: passou
- `npm run pages:dev`: passou
- `npm run pages:deploy`: falhou por ausencia do projeto remoto `signallq-pwa`
- `npm run lint`: inexistente

## Pendencias reais

- confirmar se `signallq-pwa` deve existir nesta conta Cloudflare ou em outra conta/projeto;
- criar ou vincular o projeto Pages correto antes de tentar preview/production de verdade;
- validar dominio final, HTTPS e HSTS somente depois desse alinhamento;
- opcionalmente criar uma issue propria para lint se isso passar a ser requisito de gate.
