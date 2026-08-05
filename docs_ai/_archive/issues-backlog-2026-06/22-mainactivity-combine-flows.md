## Contexto
`MainActivity.onCreate` coleta cerca de 40 valores via `collectAsStateWithLifecycle` em paralelo. Sem `distinctUntilChanged` e sem agrupamento, qualquer Flow que emite força a `AppShell` inteira a recompor — incluindo telas que nem dependem daquele valor. Em Compose isso é o vilão clássico de jank em scroll.

## Evidência
- `app/.../MainActivity.kt:67-136` — ~40 coletas paralelas; bloco 67–80 sozinho coleta 13 Flows
- Sem uso de `combine(...)` ou `derivedStateOf`

## Critério de aceite
- [ ] Criar `MainUiState` data class agregando os valores que mudam juntos
- [ ] `MainViewModel` expõe `uiState: StateFlow<MainUiState>` usando `combine(...).distinctUntilChanged().stateIn(...)`
- [ ] Coletas individuais reduzidas a no máximo 5 grupos coesos
- [ ] Onde aplicável, `derivedStateOf` para valores computados
- [ ] Medição com Layout Inspector / `Recomposer.currentRecomposerState`: redução ≥ 30% no número de recomposições durante navegação Home → Speedtest → Home
- [ ] UX visualmente idêntica

## Como verificar
- Layout Inspector → Composition counts antes/depois
- Frame time via `dumpsys gfxinfo io.veloo.app`

## Notas para o agente
- Skills: `signallq-arch`, `signallq-design`
- Cuidado: agrupar demais cria recomposição em cascata por outro motivo. Validar com instrumentação.
- Dependências: facilitado por #3 (Hilt)
