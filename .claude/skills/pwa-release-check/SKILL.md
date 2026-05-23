---
description: Checklist pré-release PWA Linka — build, manifest, Service Worker, Cloudflare Pages deploy e verificação de regressão.
---

## Quando usar
Antes de fazer deploy de uma nova versão do PWA no Cloudflare Pages.

## Checklist de build
- [ ] `npm run build` passou sem erros?
- [ ] TypeScript sem erros (`tsc --noEmit`)?
- [ ] Sem `console.log` de debug no código de produção?
- [ ] Variáveis de ambiente de produção configuradas no Cloudflare?

## Checklist de manifest PWA
- [ ] `manifest.json` atualizado com versão?
- [ ] Ícones em todos os tamanhos (192x192, 512x512)?
- [ ] `theme_color` e `background_color` corretos?
- [ ] `start_url` e `scope` configurados?
- [ ] `display: standalone` para experiência app?

## Checklist de Service Worker
- [ ] Cache version atualizado se assets mudaram?
- [ ] Estratégia de cache correta (network-first para dados, cache-first para assets)?
- [ ] Atualização de SW funciona sem deixar usuário preso em versão antiga?
- [ ] Fallback offline configurado?

## Checklist de qualidade
- [ ] Fluxo principal testado em Chrome e Safari?
- [ ] Responsivo em mobile (360px) e desktop (1280px)?
- [ ] Sem regressões visuais nas telas existentes?
- [ ] Lighthouse PWA score ≥ 80?

## Checklist de deploy (Cloudflare Pages)
- [ ] Branch correta selecionada para deploy?
- [ ] Preview deploy testado antes do production?
- [ ] `_redirects` configurado para SPA (`/* /index.html 200`)?
- [ ] Headers de segurança corretos em `_headers`?

## Limites
- Esta skill orienta, não implementa.
- Deploy e release → Renan.
- Changelog → Gema via `/changelog-update`.
