## Contexto
A injeção de dependência é feita manualmente em `MainViewModel` via blocos `lazy { Modulo.criar*() }`. Isso concentra responsabilidade, dificulta testes (não há como substituir dependências), e cria acoplamento entre o ViewModel e a construção dos módulos `core*`. Migrar para Hilt resolve com baixo custo de boilerplate e padrão amplamente conhecido por agentes de IA.

## Evidência
- `app/src/main/kotlin/io/signallq/app/kotlin/MainViewModel.kt:75-102` — `lazy { ... }` para 10+ dependências
- Módulos afetados: todos os `core*` (`coreDatabase`, `coreNetwork`, `coreDatastore`, `corePermissions`, `coreTelephony`)

## Critério de aceite
- [ ] Hilt configurado no projeto (plugin no `build.gradle.kts` raiz e em `app/build.gradle.kts`)
- [ ] `@HiltAndroidApp` na Application
- [ ] Módulos `@Module @InstallIn(SingletonComponent::class)` por área (`DatabaseModule`, `NetworkModule`, `DatastoreModule`, etc.)
- [ ] `MainViewModel` anotado `@HiltViewModel` recebendo dependências via construtor
- [ ] Blocos `lazy { Modulo.criar*() }` removidos
- [ ] Testes unitários do `MainViewModel` passam com fakes injetados
- [ ] `./gradlew test assembleDebug` verde

## Como verificar
```powershell
.\gradlew.bat test assembleDebug
```
Smoke test manual no app: launch, navegar entre tabs, rodar speedtest.

## Notas para o agente
- Skills: `signallq-arch`
- Quebrar PR por módulo se ficar grande (ex.: PR1 setup + DatabaseModule, PR2 NetworkModule, etc.)
- NÃO mudar lógica de negócio nesta issue — apenas reorganizar DI
- Dependências: bloqueia #4 (`!!`) parcialmente — facilita refactor
