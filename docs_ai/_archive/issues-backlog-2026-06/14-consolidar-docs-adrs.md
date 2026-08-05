## Contexto
Existem duas árvores de documentação: `docs/` (operacional) e `docs_ai/` (viva, para agentes). Há risco de duplicação (ex.: estrutura de projeto, fluxo de release) e ausência de ADRs documentando decisões arquiteturais importantes (sem DI, monolito de DI manual, escolha de Compose-only, etc.).

## Evidência
- `docs/PROJECT_STRUCTURE.md` vs `docs_ai/technical/`
- Ausência de `docs_ai/technical/adr/`

## Critério de aceite
- [ ] Auditoria de duplicação entre `docs/` e `docs_ai/` documentada em PR
- [ ] Regra explícita: `docs/` = operacional/release; `docs_ai/` = vivo/IA; cross-link em vez de copiar
- [ ] Criado `docs_ai/technical/adr/0001-stack-android-compose.md`
- [ ] Criado ADR para cada uma das decisões: stack escolhida, sem DI (até virar #3), modelo de monitoramento via WorkManager, design system tokens via CompositionLocal
- [ ] Template `docs_ai/technical/adr/_template.md` versionado
- [ ] Índice atualizado em `docs_ai/README.md`

## Como verificar
Manual: revisar `docs_ai/technical/adr/` tem ao menos 4 ADRs; `docs/` e `docs_ai/` não duplicam conteúdo de estrutura.

## Notas para o agente
- Skills: `signallq-docs`
- Usar formato ADR clássico (Status / Context / Decision / Consequences)
- Dependências: independente
