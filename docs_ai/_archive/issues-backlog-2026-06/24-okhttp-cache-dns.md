## Contexto
O OkHttpClient global manda `Cache-Control: no-store` em todas as requisições e não popula cache em disco. Para o payload de speedtest faz sentido; para chamadas auxiliares (DNS over HTTPS, health checks, metadata de servidores) cache curto economiza dados móveis e reduz latência percebida.

## Evidência
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:48` — `addInterceptor { ... header("Cache-Control","no-store") }`
- Sem `cache(Cache(...))` configurado

## Critério de aceite
- [ ] Criar duas instâncias: `okHttpSpeedtest` (no-store) e `okHttpAux` (com Cache de 5 MB e `Cache-Control: max-age=300` para health/DNS)
- [ ] Não cachear endpoints de medição (speedtest, ping, jitter)
- [ ] Política documentada no provedor (ou no DI module após #3)
- [ ] Validar que medições não usam resposta cacheada

## Como verificar
- Network Profiler: chamadas auxiliares repetidas em <5 min retornam 304/from-cache
- Speedtest mantém payload uncached

## Notas para o agente
- Skills: `signallq-arch`
- Impacto estimado: -5–10% dados móveis em uso típico (excluindo speedtest)
- Dependências: depende de #3 (Hilt) para separação limpa dos clients
