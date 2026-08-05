## Contexto
O `OkHttpClient` do speedtest configura `ConnectionPool(8, 60, TimeUnit.SECONDS)` globalmente. Em rede móvel, 8 conexões TCP simultâneas mantidas por 60 s significam rádio ativo mais tempo (RRC connected state), drenando bateria sem necessidade. Em Wi-Fi o custo é baixo, mas em 4G/5G é mensurável.

## Evidência
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:61` — `connectionPool(okhttp3.ConnectionPool(8, 60, TimeUnit.SECONDS))`

## Critério de aceite
- [ ] Pool adaptativo: 8/60s em Wi-Fi (mantém perf), 3/15s em mobile (libera rádio)
- [ ] Detecção via `ConnectivityManager.getNetworkCapabilities(...).hasTransport(TRANSPORT_CELLULAR)`
- [ ] Validar que speedtest mantém throughput em Wi-Fi (baseline)
- [ ] Documentar trade-off em comentário breve (uma linha) no client

## Como verificar
Battery Historian antes/depois em 5 testes consecutivos em mobile — esperar redução de tempo RRC connected pós-teste.

## Notas para o agente
- Skills: `signallq-arch`
- Impacto estimado: -5 a -10% energia em uso recorrente do speedtest em mobile
- Dependências: independente
