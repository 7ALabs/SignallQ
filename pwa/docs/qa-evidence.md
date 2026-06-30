# QA Evidence

Data da verificacao: 2026-06-30

## Comandos executados

```bash
npm ci
npm run typecheck
npm test
npm run build
npm run lint
npm run pages:dev
curl -I http://127.0.0.1:8788/
curl http://127.0.0.1:8788/
curl -i http://127.0.0.1:8788/api/speedtest/latency
npx lighthouse http://127.0.0.1:8788/
npx lighthouse http://127.0.0.1:8788/ --preset=desktop
```

## Resultado dos comandos obrigatorios

- `npm run typecheck`: aprovado
- `npm test`: aprovado, `10` arquivos de teste e `40` testes passando
- `npm run build`: aprovado
- `npm run lint`: indisponivel, script nao existe
- `npm run pages:dev`: aprovado, servidor local em `http://127.0.0.1:8788`

## Smoke local confirmado

- Home servida com `HTTP 200`
- titulo HTML: `SignallQ PWA`
- Swagger redirecionando em `/docs/swagger.html`
- endpoint `/api/speedtest/latency` respondendo `200` com `Cache-Control: no-store`

Resposta observada no endpoint de latencia:

```json
{"ok":true,"now":1782861727426,"method":"http_timing","limitations":["http_latency_not_icmp_ping"]}
```

## Lighthouse

### Mobile

- Performance: `99`
- Accessibility: `96`
- Best Practices: `100`
- SEO: `92`
- FCP: `1669 ms`
- LCP: `1978 ms`
- TBT: `0 ms`
- CLS: `0.000066`

### Desktop

- Performance: `99`
- Accessibility: `96`
- Best Practices: `100`
- SEO: `92`
- FCP: `751 ms`
- LCP: `751 ms`
- TBT: `0 ms`
- CLS: `0.000146`

## Verificacao visual

Inspecao local por screenshots em viewport mobile e desktop mostrou:

- CTA principal `Iniciar teste` visivel nos dois tamanhos
- layout sem quebra grave no estado inicial
- cards principais legiveis
- copy explicando limitacoes web logo no resumo inicial

## Limites desta rodada

- Safari/iOS: nao validado neste ambiente
- Firefox Desktop: nao validado neste ambiente
- Samsung Internet: nao validado neste ambiente
- install prompt real: nao exercitado manualmente, mas existe cobertura automatizada em `tests/install-prompt.test.ts`
- offline parcial em browser real: nao exercitado manualmente nesta rodada

## Leitura final

Para Chrome local, a base atual esta forte para SIG-51. O que falta para considerar QA final completamente encerrado e o complemento cross-browser/manual fora deste host.
