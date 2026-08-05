## Contexto
Coroutines em ViewModels usam `viewModelScope.launch { }` sem `Dispatchers` explícito, então rodam no Main por default. Para operações IO (rede, DB Room, DataStore) isso bloqueia main thread em casos extremos e dificulta teste (não há como substituir o dispatcher). O padrão correto é o repositório/data source declarar `withContext(Dispatchers.IO)` e o ViewModel receber um `CoroutineDispatcher` injetado.

## Evidência
- `app/src/main/kotlin/io/signallq/app/kotlin/MainViewModel.kt` — múltiplos `viewModelScope.launch { ... }` chamando repositórios IO
- Contraste positivo: `app/src/main/kotlin/io/signallq/app/kotlin/monitoramento/MonitoramentoWorker.kt:20-22` usa `withContext(Dispatchers.IO)` corretamente

## Critério de aceite
- [ ] Criar `DispatcherProvider` interface (default + io + main) injetada via Hilt
- [ ] Todos os DAOs/Repositórios em `core*` envolvem operações com `withContext(dispatchers.io)`
- [ ] ViewModels usam `launch(dispatchers.main)` ou deixam padrão quando lógica é apenas orquestração
- [ ] Testes substituem dispatcher por `TestDispatcher` / `UnconfinedTestDispatcher`
- [ ] Lint Detekt configurada para `BlockingMethodInNonBlockingContext`

## Como verificar
```powershell
.\gradlew.bat test
```
StrictMode com `detectDiskReads()` ativo em debug não dispara violações ao navegar pelas telas.

## Notas para o agente
- Skills: `signallq-arch`
- Dependências: depende de #3 (Hilt) para injeção limpa
