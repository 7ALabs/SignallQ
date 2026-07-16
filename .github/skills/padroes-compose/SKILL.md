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

## Limites
- Esta skill orienta, não implementa.
- Implementação → Camilo.
