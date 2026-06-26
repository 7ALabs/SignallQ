---
name: checar-release
description: Use antes de PR, beta, preview ou deploy do SignallQ PWA. Verifica build, typecheck, testes, PWA manifest, service worker, Lighthouse, Cloudflare Pages e documentação operacional. Não use para release Android.
---

# Checar Release — SignallQ PWA

## Objetivo

Evitar que o PWA seja entregue sem validação mínima.

## Checklist local

Rode o que existir:

- `npm run lint`
- `npm run typecheck`
- `npm run build`
- `npm test`

Se algum comando não existir, registre como pendência.

## Checklist PWA

Verificar quando aplicável:

- Manifest válido.
- Ícones configurados.
- Service Worker configurado sem quebrar atualização.
- App carrega em mobile.
- App carrega em desktop.
- Estados de loading, erro, vazio e sucesso existem nos fluxos tocados.
- Sem console debug desnecessário.
- Sem dependência pesada adicionada sem justificativa.

## Checklist Cloudflare Pages

Verificar quando aplicável:

- Build command correto.
- Output directory correto.
- Variáveis de ambiente não expõem segredo no browser.
- Preview deploy validado.
- Headers e redirects necessários documentados.

## Checklist de PR

Todo PR deve informar:

- Área afetada.
- Issue relacionada.
- Arquivos principais alterados.
- Comandos rodados.
- Resultado dos comandos.
- Riscos.
- Pendências.
- Se tocou fora de `pwa/`.

## Bloqueadores

Não considerar pronto se:

- build falha;
- typecheck falha;
- fluxo principal não abre;
- mexeu fora de `pwa/` sem declarar;
- inventou métrica;
- depende de segredo no browser;
- não há evidência mínima de validação.

## Saída esperada

Ao usar esta skill, entregue:

- status: aprovado, aprovado com pendência ou bloqueado;
- comandos executados;
- resultado;
- riscos;
- recomendação de merge/deploy.
