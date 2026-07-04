# Deploy Status

Data da verificacao: 2026-06-30 (atualizado 2026-07-04, GH#443/SIG-52)

## Atualizacao 2026-07-04

A decisao de arquitetura mudou: o PWA nao vai mais ter um projeto Cloudflare
Pages isolado (`signallq-pwa`, que nunca chegou a existir — ver evidencias
abaixo). Ele passa a ser publicado sob `/app` no mesmo projeto `signallq`
que tambem serve o Console Admin sob `/console` (GH#443, decisao ja tomada
na issue). Ver `deploy/pages/README.md` para o pipeline completo.

O bloqueio de producao (criar o projeto `signallq` na conta Cloudflare
correta e configurar dominio/HTTPS) continua existindo e agora e rastreado
como pendencia de infraestrutura de conta, nao de codigo — o codigo deste
PR (base path, manifest, service worker, `_redirects`/`_headers`, script de
build unificado, workflow condicional) ja esta pronto para quando o projeto
existir.

## Resumo (estado anterior a esta mudanca, mantido como historico)

O SignallQ PWA esta pronto para validacao local e para CI de PR, mas ainda nao pode ser considerado pronto para producao no Cloudflare Pages porque o projeto remoto `signallq-pwa` nao existe na conta autenticada usada nesta rodada.

## Evidencias objetivas

- `wrangler whoami`: autenticado com a conta `Giammattey, Luiz F.`
- `wrangler pages project list`: retornou apenas `signallq-admin-panel`, `ei-raiz-web` e `linka-speedtest`
- `wrangler pages deployment list --project-name signallq-pwa`: falhou com `Project not found`
- `npm run pages:deploy`: falhou com `Project not found`

## O que esta pronto

- `pwa/wrangler.jsonc` versionado
- workflow `.github/workflows/pwa-ci.yml` restrito a `pwa/**`
- script `npm run pages:deploy`
- headers estaticos em `public/_headers`
- Pages Functions validando localmente em `http://127.0.0.1:8788`

## O que continua bloqueado externamente

- preview deploy real no Pages
- deploy de producao
- validacao de dominio final
- validacao de HTTPS real
- validacao de HSTS em producao
- validacao de env vars no projeto remoto

## Leitura honesta do estado atual

Hoje o bloqueio nao esta no codigo PWA nem no CLI do Wrangler. O bloqueio esta no alinhamento entre a configuracao versionada do repo e a conta/projeto Cloudflare que deveria hospedar `signallq-pwa`.

## Proximo passo recomendado (substitui o anterior — ver GH#443/SIG-52)

1. Criar o projeto Cloudflare Pages `signallq` (nao `signallq-pwa`) na conta
   correta — via dashboard ou `wrangler pages project create signallq`.
2. Configurar as variaveis de ambiente de build necessarias (`AI_WORKER_URL`,
   `ADMIN_INGEST_URL`/`ADMIN_WORKER_URL`, `ADMIN_INGEST_KEY` para o PWA;
   `VITE_ADMIN_API_BASE_URL` para o Console — ver `deploy/pages/README.md`).
3. Rodar `node deploy/pages/build.mjs` (ou deixar o `pages-deploy.yml`
   automatizar isso) e publicar com
   `npx wrangler pages deploy deploy/pages/dist --project-name signallq`.
4. Validar `/app`, `/console`, redirect da raiz, HTTPS, headers reais e
   rollback.
5. So depois disso faz sentido seguir com dominio custom/HSTS de SIG-52.
