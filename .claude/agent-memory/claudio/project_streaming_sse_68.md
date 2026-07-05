---
name: project-streaming-sse-68
description: Issue #68 — Streaming SSE no chat inline DiagnosticoScreen. Branch criada, plano técnico postado. 5 tasks incrementais mapeadas.
metadata:
  type: project
---

Branch `feat/68-streaming-sse-chat` criada em 2026-05-26.

**Why:** Reduzir tempo de espera de 10-15s para < 2s no chat inline da DiagnosticoScreen via SSE.

**Estado do código no momento do planejamento:**
- Worker: zero suporte SSE. `env.AI.run()` síncrono, retorna JSON. Modelo padrão `@cf/google/gemma-7b-it`.
- `DiagChatEntry.kt`: `isParcial` NÃO existe — precisa ser adicionado.
- `AiDiagnosisRepository`: só tem `explainDiagnosis()` síncrono. `explainDiagnosisStream()` é criação nova.
- `MainViewModel.enviarPerguntaDiagnostico()`: fluxo linear, aguarda resposta completa.
- `DiagnosticoScreen`: sem cursor pulsante.

**5 Tasks:**
1. Worker SSE via `?stream=true` [sem owner — Renan foi descontinuado em 2026-07-04, reatribuir antes de retomar]
2. `DiagChatEntry.isParcial: Boolean = false` [Camilo]
3. `explainDiagnosisStream(): Flow<String>` aditivo no repositório [Camilo]
4. Refatorar `enviarPerguntaDiagnostico()` para stream + fallback [Camilo]
5. Cursor pulsante `DiagChatTextoComCursor` [Camilo + Lia]

**How to apply:** Ao retomar trabalho nessa issue, Tasks 1+2 são independentes. Task 5 depende só da 2. Tasks 3+4 dependem de 1 e 2.

Plano detalhado em `TEMP.md` na branch e comentário na issue: https://github.com/gmmattey/linka-android/issues/68#issuecomment-4541215323
