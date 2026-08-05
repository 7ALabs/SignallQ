## Contexto
`MonitoramentoWorker` faz medições com `HttpURLConnection` cru, sem retry estruturado e sem timeout de call global. Em redes instáveis, o Worker pode ficar segurando wake lock até `readTimeout=5s` por requisição, repetindo o desperdício a cada execução periódica.

## Evidência
- `app/.../monitoramento/MonitoramentoWorker.kt:164` — `medirLatenciaHttp()` com `connectTimeout=5000; readTimeout=5000`
- `app/.../monitoramento/MonitoramentoWorker.kt:185-186` — `medirDnsResolveTime()` mesmo padrão

## Critério de aceite
- [ ] Substituir `HttpURLConnection` por OkHttp com `callTimeout(3, SECONDS)` global e `retryOnConnectionFailure(false)`
- [ ] Detectar `NetworkCapabilities` antes de medir; abortar cedo se sem rede
- [ ] Backoff simples: se 2 medições consecutivas falharem, próxima execução adia 60 min (em vez de 30)
- [ ] WorkManager constraints: `setRequiresBatteryNotLow(true)` quando bateria < 20%
- [ ] Documentar política em comentário no `MonitoramentoScheduler`

## Como verificar
```powershell
adb shell cmd connectivity airplane-mode enable
# disparar Worker manual; deve completar em <1s sem segurar wakelock
adb shell dumpsys power | Select-String wake_lock
```

## Notas para o agente
- Skills: `signallq-arch`
- Impacto estimado: -10–15% energia em monitoramento passivo em redes ruins
- Dependências: depende de #20 (cache de config)
