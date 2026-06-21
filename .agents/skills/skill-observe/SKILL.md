---
description: Registra gaps, melhorias e padrões observados durante uma task — gera arquivo de observação para Claudete processar no ciclo de aprendizado de skills.
---

## Quando usar

Ao final de qualquer etapa de task onde o agente:
- Precisou fazer algo que não existia como skill
- Usou uma skill mas ela cobria apenas parcialmente o domínio
- Identificou um padrão recorrente sem cobertura de skill

**Não usar** se a task foi executada sem gaps — nenhuma skill faltou, tudo estava coberto. Observação vazia é ruído.

## Quem usa

Camilo, Gema, Nina, Claudete — ao final da sua etapa no pipeline `/task`, se e somente se observou gap real.

## Passos

### 1. Definir nome do arquivo de observação

```
obs-YYYY-MM-DD-[TASK-ID]-[AGENTE].md
```

Exemplo: `obs-2026-05-24-42-camilo.md`

### 2. Criar o diretório do mês se não existir

```bash
New-Item -ItemType Directory -Force ".Codex/skill-observations/$(Get-Date -Format 'yyyy-MM')"
```

### 3. Escrever o arquivo usando o template abaixo

Caminho: `.Codex/skill-observations/YYYY-MM/obs-YYYY-MM-DD-[TASK-ID]-[AGENTE].md`

### 4. Template obrigatório

```markdown
# Skill Observation — [TASK-ID] — [AGENTE] — [DATA]

## Task context
Feature: [nome da feature ou bug]
Issue: #N
Agente: [nome]

## Skill gaps found

### Missing skill (se houver — omitir seção se não houver)
Pattern: [o que precisei fazer sem referência de skill]
Trigger: [quando esse padrão volta a ocorrer]
Slug sugerido: [nome-da-skill]
Priority: alta | média | baixa
Ocorrências anteriores: [N — ou "primeira vez"]

### Incomplete skill (se houver — omitir seção se não houver)
Skill: /[nome]
Gap: [o que faltou especificamente]
Sugestão: [o que adicionar — seção, regra, exemplo]

### Skill candidata a remoção (se houver — omitir seção se não houver)
Skill: /[nome]
Motivo: [redundante com /outra | nunca aplicável para este domínio | obsoleta]

## Skills que funcionaram bem (opcional)
- /[nome] — [por quê foi útil]

## Signal summary
- [N] skills novas sugeridas | [N] skills com gap | [N] candidatas a remoção
```

## Regras críticas

- **Threshold de skill nova:** nunca sugerir criação com base em ocorrência única. Se "Ocorrências anteriores: primeira vez" → Claudete vai para o backlog, não cria. Se ≥2 ocorrências independentes → candidata real à criação.
- **Uma observação por agente por task.** Não fragmentar em múltiplos arquivos.
- Não inventar gaps. Só registrar o que realmente faltou, não o que "poderia ser útil".
- A skill candidata a remoção exige ≥2 observações independentes para ser deprecada. Uma única observação é ignorada pelo `/skill-steward`.

## Output esperado

Arquivo `.Codex/skill-observations/YYYY-MM/obs-YYYY-MM-DD-[task-id]-[agente].md` escrito e salvo.

## Limites

- Não decide o que criar ou remover — só registra o que observou. Decisão é da Claudete via `/skill-steward`.
- Não processa observações de outros agentes — cada agente escreve a sua.
- Não requer aprovação da Gema — é um arquivo de sinal, não um arquivo de skill.
