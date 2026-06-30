# CI/CD Cloudflare Pages

## Objetivo

Definir o contrato real de validacao, preview e deploy do SignallQ PWA sem misturar pipeline Android e sem prometer Cloudflare pronto quando a conta/projeto ainda nao batem.

## Estado atual em 2026-06-30

Ja existe workflow em `.github/workflows/pwa-ci.yml` com gatilho restrito a `pwa/**` e ao proprio workflow.

Esse workflow agora:

- usa `working-directory: pwa`;
- executa `npm ci`;
- executa `npm run verify`;
- tenta preview deploy em PR quando `CLOUDFLARE_API_TOKEN` e `CLOUDFLARE_ACCOUNT_ID` estiverem configurados;
- tenta production deploy em `main` com `npm run pages:deploy` nas mesmas condicoes;
- publica `pwa/dist/` como artifact.

O workflow nao tenta rodar pipeline Android.

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

- `npm run verify` executa `npm test` e `npm run build`, e o `build` inclui `tsc --noEmit`;
- `npm run pages:deploy` publica `dist` para o projeto `signallq-pwa`;
- `npm run lint` continua ausente e deve ser tratado como pendencia de infraestrutura, nao como validacao concluida.

## Variaveis e secrets

Variaveis server-side esperadas no Cloudflare Pages:

- `AI_WORKER_URL`
- `ADMIN_INGEST_URL` ou `ADMIN_WORKER_URL`
- `ADMIN_INGEST_KEY`

Para desenvolvimento local com `wrangler pages dev`, use `.dev.vars`. Esse arquivo nao deve ser versionado.

Variaveis com prefixo `VITE_` sao publicas por definicao e nao devem receber segredo, token, ingest key ou URL privada que dependa de autenticacao.

Secrets esperados no GitHub Actions:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

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
