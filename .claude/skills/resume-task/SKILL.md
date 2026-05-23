---
description: Retoma uma task de sessão anterior lendo o task file, progress log e RESUME_NEXT — sem depender da memória da conversa.
---

## Quando usar
Ao iniciar uma sessão e houver task IN_PROGRESS. Qualquer agente pode usar.

## Passos
1. **Ler task file** em `.claude/tasks/active/[TASK-ID].md`.
2. **Ler RESUME_NEXT** no task file — é o ponto exato de retomada.
3. **Rodar `git status`** para verificar estado do repositório.
4. **Ler arquivos tocados** listados no task file.
5. **Ler progress log** — últimas 5-10 entradas são suficientes.
6. **Verificar se há bloqueios** (status BLOCKED no task file).
7. **Declarar intenção** antes de agir: "Retomando [TASK-ID] a partir de [RESUME_NEXT]."

## O que NÃO fazer
- Não reler a conversa inteira para reconstruir contexto.
- Não reiniciar a task do zero sem verificar o task file primeiro.
- Não assumir estado sem conferir git status.

## Limites
- Se o task file não existir → criar antes de continuar.
- Se RESUME_NEXT estiver vago → pedir clarificação ou inferir do progress log.
