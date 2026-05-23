---
description: Verifica e limpa documentação desatualizada — detecta docs que contradizem o código atual, arquivos órfãos e referencias quebradas. Executado por Gema periodicamente.
---

## Quando usar
Gema executa quinzenalmente ou após mudanças de arquitetura significativas.

## Escopo

```
linkaAndroidKotlin/docs_ai/
linkaSpeedtestPwa/docs/  (se existir)
.claude/agents/*.md
.claude/skills/*/SKILL.md
CLAUDE.md (raiz)
```

## Checklist de higiene

### Documentação técnica
- [ ] `ANDROID_TECNICO.md` — módulos listados batem com `settings.gradle`?
- [ ] `ANDROID_FUNCNICO.md` — fluxos descritos batem com navegação atual?
- [ ] Referências a agentes aposentados removidas (Otávio, Bernardo, Cláudio, Nina como core)?
- [ ] Skills listadas no CLAUDE.md batem com as existentes em `.claude/skills/`?

### Agentes
- [ ] Agentes em `.claude/agents/` batem com os listados no CLAUDE.md?
- [ ] Nenhum agent referencia agente aposentado como obrigatório?
- [ ] Modelos declarados no frontmatter batem com a política atual?

### Skills
- [ ] Toda skill tem frontmatter com `description:`?
- [ ] Nenhuma skill referencia agente que não existe mais?
- [ ] Skills duplicadas? (verificar se duas skills cobrem o mesmo domínio)

## Ação por problema

| Problema | Ação |
|---|---|
| Doc desatualizado | Atualizar ou marcar como `[DESATUALIZADO]` no topo |
| Referência quebrada | Remover ou corrigir |
| Skill duplicada | Consolidar, manter a mais completa |
| Doc órfão (sem referência) | Mover para `docs_ai/archive/` |

## Limites
- Gema documenta problemas encontrados e corrige inconsistências simples.
- Reescritas completas de documentação → Taisa (on-demand).
