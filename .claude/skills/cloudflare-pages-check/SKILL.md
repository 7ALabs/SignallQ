---
description: Checklist de deploy e configuração Cloudflare Pages para o PWA Linka — build, variáveis de ambiente, redirects, headers e edge functions.
---

## Quando usar
Antes de configurar, modificar ou fazer deploy do PWA no Cloudflare Pages.

## Estrutura de deploy

```
linkaSpeedtestPwa/
├── public/           — assets estáticos copiados diretamente
├── src/              — código React/TypeScript
├── dist/             — output do build (não commitar)
├── _redirects        — regras de redirect (Cloudflare Pages)
├── _headers          — custom headers por rota
└── functions/        — Cloudflare Pages Functions (edge)
```

## Checklist de build
- [ ] `npm run build` passa sem erros de TypeScript?
- [ ] Build output em `dist/` sem arquivos desnecessários?
- [ ] `vite.config.ts` com `base` correto para a URL de produção?
- [ ] Variáveis de ambiente prefixadas com `VITE_` para exposição ao cliente?
- [ ] Segredos de servidor em Functions (não expostos ao cliente)?

## Checklist de redirects (`_redirects`)
- [ ] SPA redirect configurado: `/* /index.html 200`?
- [ ] Redirects específicos de rotas antes do catch-all?
- [ ] Trailing slash tratado consistentemente?

## Checklist de headers (`_headers`)
- [ ] Cache-Control configurado para assets estáticos (`/assets/*`)?
- [ ] Service Worker com `Cache-Control: no-cache`?
- [ ] CSP (Content Security Policy) configurado?
- [ ] `X-Frame-Options: DENY` para prevenir clickjacking?

## Cloudflare Pages Functions
- Ficam em `functions/` — executam no edge (Cloudflare Workers runtime)
- Sem Node.js APIs — usar Web APIs padrão
- Sem estado entre requests — KV para persistência
- Timeout de 50ms CPU em plano gratuito

## Variáveis de ambiente
- Configuradas no dashboard Cloudflare Pages (não em `.env` commitado)
- `VITE_*` → expostas ao bundle do cliente
- Sem `VITE_` → apenas disponíveis em Functions/server
- Nunca commitar `.env.production` com segredos

## Limites
- Esta skill orienta, não implementa.
- Implementação → Renan.
