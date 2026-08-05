## Contexto
O app não tem Baseline Profile nem configuração explícita de splits AAB. Baseline Profile reduz cold start em 20-30% no primeiro launch após install, especialmente relevante para um app de diagnóstico onde o usuário abre, faz a tarefa, fecha. Splits por ABI/densidade reduzem tamanho do APK distribuído.

## Evidência
- `app/build.gradle.kts` — sem bloco `baselineProfile` nem `bundle { abi { enableSplit = true } }`

## Critério de aceite
- [ ] Módulo `:baselineprofile` criado com Macrobenchmark
- [ ] Profile gerador cobrindo: launch → Home → cada tab → speedtest → diagnóstico
- [ ] `baselineProfile` task gera arquivo em `app/src/main/baseline-prof.txt`
- [ ] R8 consome o profile (verificar em `app/build/outputs/mapping/release/`)
- [ ] AAB com splits por ABI (arm64-v8a, armeabi-v7a, x86_64) e densidade
- [ ] Métricas antes/depois documentadas (`docs_ai/operations/PERFORMANCE.md`)

## Como verificar
```powershell
.\gradlew.bat :baselineprofile:pixel6Api33BenchmarkAndroidTest
.\gradlew.bat bundleRelease
# medir cold start com Macrobenchmark
```

## Notas para o agente
- Skills: `signallq-arch`
- Requer device físico ou emulador AOSP (gms causa ruído na medição)
- Dependências: independente
