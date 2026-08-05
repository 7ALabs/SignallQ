## Contexto
Não há um padrão único de UiState entre telas Compose, levando a tratamento inconsistente de loading/erro/vazio. Cada feature reimplementa um booleano `isLoading` + nullable de erro. Um sealed class `UiState<T>` e um componente `StatefulScreen` reduzem boilerplate e garantem UX consistente.

## Evidência
- Telas de feature em `feature*/src/main/kotlin/.../*.kt` — padrão variado
- Não há `Empty` state explícito em várias telas

## Critério de aceite
- [ ] Criar `app/src/main/kotlin/io/signallq/app/kotlin/ui/state/UiState.kt` com sealed `Loading | Success(data) | Empty | Error(throwable, retry)`
- [ ] Criar componente `StatefulScreen(uiState, onRetry) { data -> ... }` em `ui/component/`
- [ ] Migrar ao menos 3 telas principais (Home, Speedtest, Diagnóstico) para o novo padrão
- [ ] Empty states com ilustração + CTA (alinhado ao design system)
- [ ] Error states com mensagem + botão Retry padronizado
- [ ] ViewModels expõem `StateFlow<UiState<T>>` em vez de campos separados

## Como verificar
Smoke test manual: desligar Wi-Fi → cada tela mostra Error com retry; primeira abertura → Loading; lista vazia → Empty state.

## Notas para o agente
- Skills: `signallq-arch`, `signallq-design`
- Manter migração incremental — não bloquear PR esperando 100% das telas
- Dependências: independente
