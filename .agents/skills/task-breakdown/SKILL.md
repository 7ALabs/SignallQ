---
description: Quebra uma user story em tasks pequenas, independentes e executáveis por um agente específico. Cada task deve caber em uma sessão de trabalho focada.
---

## Quando usar
Após `/refine-story`. Antes de distribuir trabalho aos agentes.

## Passos
1. **Identificar domínios** da story: Android / PWA / UX / QA / Docs.
2. **Chamar Marcelo** para mapear arquivos e módulos afetados antes de planejar.
3. **Criar tasks por domínio**, garantindo que cada uma:
   - Toca no máximo 1 módulo principal.
   - Tem agente responsável claro.
   - Tem critério de aceite verificável.
   - É independente das outras (ou tem ordem explícita).
4. **Verificar WIP** de cada agente antes de atribuir.
5. **Ordenar execução**: o que bloqueia o quê.
6. **Criar task files** em `.Codex/tasks/active/` ou `.Codex/tasks/queue/<agente>/`.

## Task size limits
- Android/PWA: ≤1 módulo principal, ≤2 dias de trabalho.
- UX: ≤3 telas por revisão.
- QA: ≤1 feature por ciclo de QA.
- Marcelo: ≤5 arquivos por task de dev.

## Output esperado
Lista de tasks no formato:
```
TASK-001: [título]
  Agente: Camilo
  Escopo: [o que fazer]
  Aceite: [como verificar]
  Bloqueia: TASK-003
  Task file: .Codex/tasks/active/TASK-001.md
```

## Limites
- Não criar task que mistura Android e PWA no mesmo agente.
- Não criar task vaga ("melhorar o diagnóstico" não é task).
- Refactor amplo exige aprovação explícita antes de virar task.
