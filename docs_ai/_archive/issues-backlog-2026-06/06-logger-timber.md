## Contexto
35+ chamadas diretas a `android.util.Log` espalhadas no código, com tags hardcoded por arquivo. Isso impede: desligar logs em release, plugar crash reporter (Crashlytics/Sentry), padronizar formato, e instrumentar testes. Timber resolve com um wrapper leve e amplamente conhecido.

## Evidência
- `app/src/main/kotlin/io/signallq/app/kotlin/monitoramento/MonitoramentoWorker.kt:43,85,174` — `Log.w("LinkaMonitor", ...)`, `Log.d("MainViewModel", ...)`
- Mapear todos: `Select-String -Path **/*.kt -Pattern 'Log\.[dewiv]\('`

## Critério de aceite
- [ ] Dependência Timber adicionada (`com.jakewharton.timber:timber`)
- [ ] `Timber.plant(DebugTree())` em build debug; `ReleaseTree` filtrando WARN/ERROR em release
- [ ] Todas as chamadas `Log.*` substituídas por `Timber.*`
- [ ] Tags automáticas (Timber infere classe) — remover tags manuais
- [ ] Regra Detekt para barrar `import android.util.Log`
- [ ] `./gradlew test assembleDebug assembleRelease` verde

## Como verificar
```powershell
Get-ChildItem -Recurse -Include *.kt -Path .\app\src\main,.\core*\src\main,.\feature*\src\main | Select-String -Pattern 'android\.util\.Log'
# deve retornar vazio
```

## Notas para o agente
- Skills: `signallq-arch`
- Manter ReleaseTree pronto para plugar Crashlytics no futuro (issue separada se necessário)
- Dependências: independente
