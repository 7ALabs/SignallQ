## Contexto
Há 15+ usos de `!!` (not-null assertion) em código de produção. Cada `!!` é uma `NullPointerException` em potencial e indica que o tipo nullable não está sendo tratado de forma segura. A erradicação combina refactor manual com uma regra Detekt para evitar regressões.

## Evidência
- `app/src/main/kotlin/io/signallq/app/kotlin/MainViewModel.kt:135` — `apelido!!`
- `app/src/main/kotlin/io/signallq/app/kotlin/MainViewModel.kt:409` — `ultimoBenchmarkDnsEpochMs!!`
- `app/src/main/kotlin/io/signallq/app/kotlin/ui/screen/HomeScreen.kt:422-424` — `effectiveTs!!`, `effectiveDl!!`, `effectiveUl!!`
- Comando para mapear todos: `Select-String -Path **/*.kt -Pattern '!!' -CaseSensitive`

## Critério de aceite
- [ ] Zero `!!` em `src/main` (testes podem manter, com justificativa)
- [ ] Padrões aplicados: `let { }`, `?.run { }`, `requireNotNull(x) { "mensagem clara" }`, smart-cast com `if (x != null)`
- [ ] Regra Detekt `ExplicitItLambdaParameter` + `UnsafeCallOnNullableType` habilitadas e configuradas como `error`
- [ ] Build falha se um novo `!!` for introduzido
- [ ] Testes existentes continuam passando

## Como verificar
```powershell
.\gradlew.bat detekt
Get-ChildItem -Recurse -Include *.kt -Path .\app\src\main,.\core*\src\main,.\feature*\src\main | Select-String -Pattern '!!' -CaseSensitive
```

## Notas para o agente
- Skills: `signallq-arch`
- Para cada `!!`: entender por que o valor poderia ser não-nulo naquele ponto e expressar isso explicitamente (early return, requireNotNull, ou tipo não-nullable a montante)
- Dependências: facilitado por #3 (Hilt), mas pode rodar independente
