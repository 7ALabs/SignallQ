## Contexto
`MonitoramentoWorker.doWork()` lê preferências do DataStore via `.first()` 8 vezes em sequência. Cada `.first()` bloqueia até a próxima emissão; sob Doze Mode com concorrência de escrita, pode somar 100+ ms só em leitura, consumindo CPU/wakelock à toa em um Worker que roda a cada 30 min.

## Evidência
- `app/.../monitoramento/MonitoramentoWorker.kt:108-111` — 4 `.first()` sequenciais (alertas)
- `app/.../monitoramento/MonitoramentoWorker.kt:130-133` — 4 `.first()` sequenciais (thresholds)

## Critério de aceite
- [ ] Criar `MonitoramentoConfig` data class que agrega todos os flags/thresholds
- [ ] Repositório expõe `configFlow: Flow<MonitoramentoConfig>` (combine interno)
- [ ] Worker lê uma única vez: `val cfg = repo.configFlow.first()`
- [ ] Tempo de execução do Worker medido antes/depois (Trace + WorkManager observer)
- [ ] Comportamento funcional idêntico (mesmos eventos emitidos)

## Como verificar
```powershell
.\gradlew.bat :app:test --tests "*MonitoramentoWorker*"
adb shell dumpsys jobscheduler | Select-String LinkaMonitor
```
Trace via `Trace.beginSection("MonitoramentoWorker.doWork")` antes/depois.

## Notas para o agente
- Skills: `signallq-arch`
- Impacto estimado: -50–80 ms por execução do Worker; -3–5% energia em uso diário
- Dependências: facilita #21
