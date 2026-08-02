---
description: Checklist e padrões de implementação Jetpack Compose para o SignallQ — estrutura de Screen, ViewModel, StateFlow, estados visuais e anti-padrões a evitar.
---

## Quando usar
Antes de implementar ou revisar código Compose no SignallQ.

## Padrões obrigatórios

### Estrutura
- Screen = função Composable que recebe apenas `uiState` e callbacks. Sem lógica de negócio.
- ViewModel expõe `StateFlow<UiState>` imutável. Nunca `MutableState` público.
- UiState = data class sealed ou data class com campos opcionais para cada estado.

### Estados visuais
Toda Screen deve tratar:
- `Loading` — spinner ou skeleton, nunca tela em branco.
- `Success` — conteúdo principal.
- `Error` — mensagem + ação de retry se aplicável.
- `Empty` — estado vazio com microcopy + ação se aplicável.

### Composables
- Funções pequenas e focadas. Uma responsabilidade por Composable.
- Parâmetros explícitos — sem usar ViewModel diretamente dentro de Composable filho.
- `remember` e `LaunchedEffect` apenas quando necessário.
- Animações com `AnimatedVisibility` ou `Crossfade` — não animar manualmente com handler.

### Anti-padrões
- ❌ Chamar use case ou repository diretamente de Composable.
- ❌ Usar `GlobalScope.launch` em ViewModel.
- ❌ Duplicar Composable existente sem verificar se já existe.
- ❌ Hardcodar cor sem usar token do tema.
- ❌ `feature*` dependendo de outro `feature*`.

### DI e instanciação (absorvido de `arquitetura-android`, fundida em 2026-07-23)
- Hilt em toda a cadeia. `@Singleton` para repositórios e clientes HTTP compartilhados — nunca instanciar manualmente (ex.: `AiDiagnosisRepository`, `OkHttpClient` de UPnP/scan, via `AppModule`).
- ViewModel por feature via `@HiltViewModel` — sem god ViewModel. Exceções documentadas: `MainViewModel` (estado de navegação) e `ChatDiagnosticoIaViewModel` (chat de IA), justificadas por escopo transversal real.
- `SignallQOrchestrator` vive em `:featureDiagnostico` — nunca mover para `:app`.
- URL de Worker Cloudflare sempre via `BuildConfig`, nunca hardcoded.
- Para módulos Gradle reais (contagem, nomes, dependências permitidas) e identificadores técnicos preservados, a fonte é `.claude/CLAUDE.md` (seção Identidade) e `.claude/rules/higiene-e-padronizacao-repositorio.md` (seção 5) — não duplicar aqui, eles mudam com o projeto e esta skill não é a fonte de verdade desses números.

## Limites
- Esta skill orienta, não implementa.
- Implementação → Camilo.
